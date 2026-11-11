package app.levitx.core.session

import app.levitx.core.load.AddedWeight
import app.levitx.core.load.BandAssistance
import app.levitx.core.load.EffectiveLoad
import app.levitx.core.load.LoadLimits
import app.levitx.core.load.LoadModifier
import app.levitx.core.load.calculateEffectiveLoad
import app.levitx.core.skills.Gate

/**
 * One set as the person did it.
 *
 * @property value Reps or seconds done.
 * @property repsInReserve How many more reps they think they had left, or null
 *   for holds and when they didn't say.
 * @property isBestEffort True for the session's best-effort set.
 * @property addedKg Weight carried: a vest, or a belt with plates. 0 for none.
 * @property assistanceKg Help from a band, as calibrated. 0 for none.
 * @throws IllegalArgumentException if a number is negative, or a modifier is
 *   above [LoadLimits.MAX_MODIFIER_KG].
 */
public data class LoggedSet(
    val value: Int,
    val repsInReserve: Int?,
    val isBestEffort: Boolean,
    val addedKg: Double = 0.0,
    val assistanceKg: Double = 0.0,
) {
    init {
        require(value >= 0) { "value must not be negative" }
        require(repsInReserve == null || repsInReserve >= 0) { "repsInReserve must not be negative" }
        require(addedKg.isValidModifier()) { "addedKg out of range" }
        require(assistanceKg.isValidModifier()) { "assistanceKg out of range" }
    }

    /**
     * True when the set counts toward the gate. Band-assisted sets build
     * toward a gate but don't pass it: "10 pull-ups" means 10 without help.
     * Added weight makes a set harder, so weighted sets do count.
     */
    val countsTowardGate: Boolean
        get() = assistanceKg == 0.0

    /** The load modifiers this set used, for [calculateEffectiveLoad]. */
    val modifiers: List<LoadModifier>
        get() = buildList {
            if (addedKg > 0.0) add(AddedWeight(addedKg))
            if (assistanceKg > 0.0) add(BandAssistance(assistanceKg))
        }

    /**
     * Kilograms moved per rep in this set, or null when the gate has no load
     * profile (holds) or the bodyweight isn't known.
     */
    public fun loadPerRep(gate: Gate, bodyweightKg: Double?): EffectiveLoad? {
        val profile = gate.loadProfile ?: return null
        return bodyweightKg?.let { calculateEffectiveLoad(it, profile, modifiers) }
    }
}

/**
 * A finished session, ready to save.
 *
 * @property gateId The gate the session trained.
 * @property startedAtEpochMillis When the first set started, in UTC milliseconds.
 * @property finishedAtEpochMillis When the session was finished, in UTC milliseconds.
 * @property sets The sets in the order they were done. At least one.
 * @property bodyweightKg Bodyweight when the session was done, if known, so loads
 *   can be worked out later even after bodyweight changes.
 */
public data class CompletedSession(
    val gateId: String,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val sets: List<LoggedSet>,
    val bodyweightKg: Double? = null,
) {
    init {
        require(sets.isNotEmpty()) { "a session needs at least one set" }
        require(finishedAtEpochMillis >= startedAtEpochMillis) { "a session can't finish before it starts" }
    }

    /**
     * The best set that counts toward the gate, which becomes the gate's best
     * if it beats the old one. 0 when every set was band-assisted.
     */
    val best: Int
        get() = sets.filter { it.countsTowardGate }.maxOfOrNull { it.value } ?: 0
}

/** True for a vest, belt or band figure the load rules accept. */
internal fun Double.isValidModifier(): Boolean = isFinite() && this in 0.0..LoadLimits.MAX_MODIFIER_KG
