package app.levitx.data.equipment

import kotlinx.coroutines.flow.Flow

/** Longest band name accepted. Names are labels like "Red", not notes. */
const val MAX_BAND_NAME_LENGTH: Int = 30

/**
 * A resistance band and the help it gives.
 *
 * @property id Stable id.
 * @property name The person's name for it, usually its colour.
 * @property assistanceKg Kilograms of help, from calibration.
 */
data class Band(val id: String, val name: String, val assistanceKg: Double)

/** The person's bodyweight and bands: what's needed to work out the load of each rep. */
interface EquipmentRepository {
    /** The latest bodyweight in kilograms, or null before one is entered. */
    fun observeBodyweightKg(): Flow<Double?>

    /**
     * Records a bodyweight reading.
     *
     * @throws IllegalArgumentException outside `LoadLimits` (20 to 350 kg).
     */
    suspend fun recordBodyweight(kg: Double)

    /** Every band, oldest first. */
    fun observeBands(): Flow<List<Band>>

    /**
     * Adds a band.
     *
     * @throws IllegalArgumentException if [name] is blank or [assistanceKg] is outside (0, 200].
     */
    suspend fun addBand(name: String, assistanceKg: Double)

    /**
     * Replaces every band with [bands], keeping their ids and order. The watch
     * uses this to mirror the bands set up on the phone.
     *
     * @throws IllegalArgumentException if any band has a blank or over-long name, or
     *   assistance outside (0, 200].
     */
    suspend fun replaceBands(bands: List<Band>)

    /** Removes a band. Sets already logged with it keep their numbers. */
    suspend fun deleteBand(id: String)
}
