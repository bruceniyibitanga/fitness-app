package app.levitx.wear.tile

import app.levitx.core.skills.SkillCatalogue
import app.levitx.testing.skillSession
import app.levitx.wear.home.NextSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class TileContentTest {
    @Test
    fun noSessionPointsToThePhone() {
        assertEquals(TileContent.NoSession, tileContentFor(null))
    }

    @Test
    fun showsWhatTheNextSessionHolds() {
        val session = skillSession(SkillCatalogue.muscleUp.gates[1])

        assertEquals(
            TileContent.NextSession(NextSummary(2, session.estimatedMinutes, session.plans.first().exercise.name)),
            tileContentFor(session),
        )
    }
}
