package app.levitx.testing

import app.levitx.data.progress.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [ProgressRepository] with the same keep-the-highest rule as the real one. */
class FakeProgressRepository(initial: Map<String, Int> = emptyMap()) : ProgressRepository {
    private val bests = MutableStateFlow(initial)

    override fun observeBests(): Flow<Map<String, Int>> = bests.asStateFlow()

    override suspend fun recordBest(gateId: String, best: Int) {
        bests.update { current -> if (best > (current[gateId] ?: 0)) current + (gateId to best) else current }
    }

    /** The current bests, for assertions. */
    val current: Map<String, Int>
        get() = bests.value
}
