package app.levitx.data.equipment

import androidx.room.withTransaction
import app.levitx.core.load.LoadLimits
import app.levitx.data.database.BandEntity
import app.levitx.data.database.BodyweightEntity
import app.levitx.data.database.LevitxDatabaseProvider
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Stores bodyweight and bands in the encrypted database. Bodyweight is health data. */
@Singleton
internal class RoomEquipmentRepository @Inject constructor(private val databaseProvider: LevitxDatabaseProvider) :
    EquipmentRepository {
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
        requireValidBand(trimmed, assistanceKg)
        databaseProvider.database().equipmentDao().insertBand(
            BandEntity(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                assistanceKg = assistanceKg,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun replaceBands(bands: List<Band>) {
        val now = System.currentTimeMillis()
        val rows = bands.mapIndexed { index, band ->
            val trimmed = band.name.trim()
            requireValidBand(trimmed, band.assistanceKg)
            require(band.id.isNotBlank()) { "band id must not be blank" }
            // Bands are listed by creation time, so stepping it keeps the given order.
            BandEntity(band.id, trimmed, band.assistanceKg, createdAtEpochMillis = now + index)
        }
        val database = databaseProvider.database()
        database.withTransaction {
            database.equipmentDao().deleteAllBands()
            database.equipmentDao().insertBands(rows)
        }
    }

    override suspend fun deleteBand(id: String) {
        databaseProvider.database().equipmentDao().deleteBand(id)
    }
}

private fun requireValidBand(trimmedName: String, assistanceKg: Double) {
    require(trimmedName.isNotEmpty() && trimmedName.length <= MAX_BAND_NAME_LENGTH) {
        "band name must be 1 to $MAX_BAND_NAME_LENGTH characters"
    }
    require(assistanceKg.isFinite() && assistanceKg > 0.0 && assistanceKg <= LoadLimits.MAX_MODIFIER_KG) {
        "band assistance out of range"
    }
}
