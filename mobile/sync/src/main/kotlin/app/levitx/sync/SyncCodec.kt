package app.levitx.sync

import app.levitx.core.load.LoadLimits
import app.levitx.core.session.CompletedSession
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.equipment.Band
import app.levitx.data.equipment.MAX_BAND_NAME_LENGTH
import app.levitx.data.session.StoredSession
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Version of the wire format below. Bump it for any change an older app couldn't read. */
internal const val WIRE_VERSION = 1

/** Data items are capped at 100 KB by the Data Layer. Ours are far smaller; anything bigger isn't ours. */
internal const val MAX_PAYLOAD_BYTES = 64 * 1024

/** Most sets a synced session can have. Plans have four; this leaves room to grow. */
internal const val MAX_SETS = 20

/** Most bands accepted from the phone. */
internal const val MAX_BANDS = 50

/** Largest reps, seconds or best accepted. Far above anything a person can do. */
internal const val MAX_RESULT = 10_000

private const val MAX_ID_LENGTH = 64
private val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")

/** The phone's state as it travels. Field names are short because every byte crosses Bluetooth. */
@Serializable
internal data class WireState(
    val v: Int,
    val pathId: String,
    val bests: Map<String, Int> = emptyMap(),
    val bands: List<WireBand> = emptyList(),
)

/** A band as it travels. */
@Serializable
internal data class WireBand(val id: String, val name: String, val kg: Double)

/** A finished session as it travels. Bodyweight is not included: it never leaves the phone. */
@Serializable
internal data class WireSession(
    val v: Int,
    val id: String,
    val gateId: String,
    val startedAt: Long,
    val finishedAt: Long,
    val sets: List<WireSet>,
)

/** A logged set as it travels. */
@Serializable
internal data class WireSet(
    val value: Int,
    val rir: Int? = null,
    val bestEffort: Boolean = false,
    val addedKg: Double = 0.0,
    val assistKg: Double = 0.0,
)

/** The result of reading a payload that arrived from the other device. */
internal sealed interface Decoded<out T> {
    /** The payload was valid. */
    data class Ok<T>(val value: T) : Decoded<T>

    /**
     * The payload was written by a newer version of the app. It is left alone
     * so this device can read it after it is updated.
     */
    data object NewerVersion : Decoded<Nothing>

    /** The payload is not something this app wrote, or its values are impossible. It is ignored. */
    data object Invalid : Decoded<Nothing>
}

/**
 * Turns sync data into bytes and back.
 *
 * Nothing that arrives is trusted. Every payload is checked for size, version
 * and sensible values before any of it reaches the database, and anything that
 * fails is dropped whole. Decoding never throws.
 */
internal object SyncCodec {
    // Unknown keys are ignored so a newer app can add optional fields without
    // breaking an older one. Changes an older app can't ignore bump WIRE_VERSION.
    private val json = Json { ignoreUnknownKeys = true }

    /** Encodes the phone's state. */
    fun encodeState(state: PhoneState): ByteArray = json.encodeToString(
        WireState.serializer(),
        WireState(
            v = WIRE_VERSION,
            pathId = state.path.id,
            bests = state.bests,
            bands = state.bands.map { WireBand(it.id, it.name, it.assistanceKg) },
        ),
    ).encodeToByteArray()

    /** Decodes and checks the phone's state. */
    fun decodeState(payload: ByteArray): Decoded<PhoneState> = decode(payload, WireState.serializer()) { wire ->
        val path = SkillCatalogue.byId(wire.pathId)
        val bandsAreValid = wire.bands.size <= MAX_BANDS && wire.bands.all { it.isValid() }
        val bestsAreValid = wire.bests.values.all { it in 0..MAX_RESULT }
        if (path == null || !bandsAreValid || !bestsAreValid) {
            null
        } else {
            PhoneState(
                path = path,
                // A best for a gate this app doesn't know is no use to it, so it is left out.
                bests = wire.bests.filterKeys { SkillCatalogue.gateById(it) != null },
                bands = wire.bands.map { Band(it.id, it.name.trim(), it.kg) },
            )
        }
    }

    /** Encodes a finished session. */
    fun encodeSession(stored: StoredSession): ByteArray = json.encodeToString(
        WireSession.serializer(),
        WireSession(
            v = WIRE_VERSION,
            id = stored.id,
            gateId = stored.session.gateId,
            startedAt = stored.session.startedAtEpochMillis,
            finishedAt = stored.session.finishedAtEpochMillis,
            sets = stored.session.sets.map { set ->
                WireSet(set.value, set.repsInReserve, set.isBestEffort, set.addedKg, set.assistanceKg)
            },
        ),
    ).encodeToByteArray()

    /** Decodes and checks a finished session. */
    fun decodeSession(payload: ByteArray): Decoded<StoredSession> = decode(payload, WireSession.serializer()) { wire ->
        val idIsValid = UUID_PATTERN.matches(wire.id)
        val gateIsKnown = SkillCatalogue.gateById(wire.gateId) != null
        val setsAreValid = wire.sets.size in 1..MAX_SETS && wire.sets.all { it.value in 0..MAX_RESULT }
        val isValid = idIsValid && gateIsKnown && setsAreValid && wire.startedAt > 0
        if (!isValid) {
            null
        } else {
            // The :core types check the rest (negative numbers, load limits,
            // finishing before starting) and throw, which decode() turns into Invalid.
            StoredSession(
                id = wire.id.lowercase(),
                session = CompletedSession(
                    gateId = wire.gateId,
                    startedAtEpochMillis = wire.startedAt,
                    finishedAtEpochMillis = wire.finishedAt,
                    sets = wire.sets.map { LoggedSet(it.value, it.rir, it.bestEffort, it.addedKg, it.assistKg) },
                ),
            )
        }
    }

    /**
     * Shared decoding steps: size, JSON, version, then [validate], which
     * returns null (or throws) for values that make no sense.
     */
    private fun <W, T : Any> decode(
        payload: ByteArray,
        serializer: DeserializationStrategy<W>,
        validate: (W) -> T?,
    ): Decoded<T> {
        if (payload.isEmpty() || payload.size > MAX_PAYLOAD_BYTES) return Decoded.Invalid
        return runCatching<Decoded<T>> {
            val element = json.parseToJsonElement(payload.decodeToString())
            val version = element.jsonObject["v"]?.jsonPrimitive?.intOrNull
            when {
                version == null || version < 1 -> Decoded.Invalid

                version > WIRE_VERSION -> Decoded.NewerVersion

                else -> validate(json.decodeFromJsonElement(serializer, element))?.let { Decoded.Ok(it) }
                    ?: Decoded.Invalid
            }
        }.getOrDefault(Decoded.Invalid)
    }

    private fun WireBand.isValid(): Boolean = id.isNotBlank() &&
        id.length <= MAX_ID_LENGTH &&
        name.trim().length in 1..MAX_BAND_NAME_LENGTH &&
        kg.isFinite() &&
        kg > 0.0 &&
        kg <= LoadLimits.MAX_MODIFIER_KG
}
