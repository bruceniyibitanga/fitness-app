package app.levitx.data.equipment

import app.levitx.core.load.LoadLimits
import app.levitx.data.database.BandEntity
import app.levitx.data.database.BodyweightEntity
import app.levitx.data.database.LevitxDatabaseProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Longest band name accepted. Names are labels like "Red", not notes. */
internal const val MAX_BAND_NAME_LENGTH = 30

/** Stores bodyweight and bands in the encrypted database. Bodyweight is health data. */
@Singleton
internal class RoomEquipmentRepository @Inject constructor(
    private val databaseProvider: LevitxDatabaseProvider,
) : EquipmentRepository {
    override fun observeBodyweightKg(): Flow<Double?> = flow {
        emitAll(databaseProvider.database().equipmentDao().observeLatestBodyweight())
    }.distinctUntilChanged()

    override suspend fun recordBodyweight(kg: Double) {
        require(kg.isFinite() && kg in LoadLimits.MIN_BODYWEIGHT_KG..LoadLimits.MAX_BODYWEIGHT_KG) {
            "bodyweight out of range"
        }
        databaseProvider.database().equipmentDao().insertBodyweight(
            BodyweightEntity(kg = kg, recordedAtEpochMillis = System.currentTimeMillis()),
        )
    }

    override fun observeBands(): Flow<List<Band>> = flow {
        emitAll(databaseProvider.database().equipmentDao().observeBands())
    }
        .map { rows -> rows.map { Band(it.id, it.name, it.assistanceKg) } }
        .distinctUntilChanged()

    override suspend fun addBand(name: String, assistanceKg: Double) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= MAX_BAND_NAME_LENGTH) {
            "band name must be 1 to $MAX_BAND_NAME_LENGTH characters"
        }
        require(assistanceKg.isFinite() && assistanceKg > 0.0 && assistanceKg <= LoadLimits.MAX_MODIFIER_KG) {
            "band assistance out of range"
        }
        databaseProvider.database().equipmentDao().insertBand(
            BandEntity(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                assistanceKg = assistanceKg,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteBand(id: String) {
        databaseProvider.database().equipmentDao().deleteBand(id)
    }
}
