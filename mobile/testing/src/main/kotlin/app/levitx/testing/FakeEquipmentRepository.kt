package app.levitx.testing

import app.levitx.data.equipment.Band
import app.levitx.data.equipment.EquipmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [EquipmentRepository]. */
class FakeEquipmentRepository(bodyweightKg: Double? = null, bands: List<Band> = emptyList()) : EquipmentRepository {
    private val bodyweight = MutableStateFlow(bodyweightKg)
    private val bandList = MutableStateFlow(bands)
    private var nextId = 0

    override fun observeBodyweightKg(): Flow<Double?> = bodyweight.asStateFlow()

    override suspend fun recordBodyweight(kg: Double) {
        bodyweight.value = kg
    }

    override fun observeBands(): Flow<List<Band>> = bandList.asStateFlow()

    override suspend fun addBand(name: String, assistanceKg: Double) {
        bandList.update { it + Band(id = "band-${nextId++}", name = name, assistanceKg = assistanceKg) }
    }

    override suspend fun deleteBand(id: String) {
        bandList.update { bands -> bands.filterNot { it.id == id } }
    }

    /** Current bands, for assertions. */
    val bands: List<Band>
        get() = bandList.value
}
