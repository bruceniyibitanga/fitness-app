package app.levitx.core.session

import app.levitx.core.skills.SkillCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoadedSetTest {
    private val pullUps = SkillCatalogue.muscleUp.gates.first { it.id == "muscle_up.pull_ups" }
    private val deadHang = SkillCatalogue.muscleUp.gates.first()

    @Test
    fun bandAssistedSetsDontCountTowardTheGate() {
        assertFalse(LoggedSet(10, 0, true, assistanceKg = 20.0).countsTowardGate)
        assertTrue(LoggedSet(10, 0, true, addedKg = 10.0).countsTowardGate)
    }

    @Test
    fun sessionBestIgnoresAssistedSets() {
        val session = CompletedSession(
            gateId = pullUps.id,
            startedAtEpochMillis = 0,
            finishedAtEpochMillis = 1,
            sets = listOf(LoggedSet(9, 0, false, assistanceKg = 15.0), LoggedSet(6, 0, true)),
        )
        assertEquals(6, session.best)
    }

    @Test
    fun allAssistedSessionHasNoBest() {
        val session = CompletedSession(pullUps.id, 0, 1, listOf(LoggedSet(5, 1, true, assistanceKg = 20.0)))
        assertEquals(0, session.best)
    }

    @Test
    fun loadPerRepUsesTheGatesProfileAndModifiers() {
        val load = assertNotNull(LoggedSet(5, 2, false, addedKg = 10.0).loadPerRep(pullUps, bodyweightKg = 74.0))
        assertEquals(84.0, load.totalKg, absoluteTolerance = 0.001)
        assertTrue(load.isEstimate)
    }

    @Test
    fun noLoadForHoldsOrWithoutBodyweight() {
        assertNull(LoggedSet(20, null, false).loadPerRep(deadHang, 74.0))
        assertNull(LoggedSet(5, 2, false).loadPerRep(pullUps, null))
    }

    @Test
    fun rejectsModifiersOutsideTheLimits() {
        assertFailsWith<IllegalArgumentException> { LoggedSet(5, 2, false, addedKg = -1.0) }
        assertFailsWith<IllegalArgumentException> { LoggedSet(5, 2, false, assistanceKg = 500.0) }
    }
}
