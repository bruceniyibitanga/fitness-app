package app.levitx.data.progress

import app.levitx.core.skills.SkillCatalogue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryProgressRepositoryTest {
    private val path = SkillCatalogue.muscleUp
    private val firstGate = path.gates.first()

    @Test
    fun newPersonStartsAtTheFirstGate() = runTest {
        val progress = InMemoryProgressRepository().observeProgress(path).first()
        assertEquals(0, progress.currentIndex)
    }

    @Test
    fun recordingABestUpdatesProgress() = runTest {
        val repository = InMemoryProgressRepository()
        repository.recordBest(firstGate.id, firstGate.target)
        assertEquals(1, repository.observeProgress(path).first().currentIndex)
    }

    @Test
    fun weakerAttemptDoesNotLowerTheBest() = runTest {
        val repository = InMemoryProgressRepository()
        repository.recordBest(firstGate.id, firstGate.target)
        repository.recordBest(firstGate.id, 1)
        assertEquals(1, repository.observeProgress(path).first().currentIndex)
    }
}
