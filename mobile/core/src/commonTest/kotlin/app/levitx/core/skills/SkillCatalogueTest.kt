package app.levitx.core.skills

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.MovementPattern
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.SkillTraining
import app.levitx.core.coach.atDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The programs on the Train tab. */
class SkillCatalogueTest {
    private val everything = Equipment.entries.toSet()

    @Test
    fun thereAreEighteenProgramsWithUniqueIds() {
        assertEquals(18, SkillCatalogue.all.size)
        assertEquals(18, SkillCatalogue.all.map { it.id }.toSet().size)
    }

    @Test
    fun theFirstTwoProgramsKeepTheirGateIdsSoSavedProgressStillMatches() {
        assertEquals(
            listOf("dead_hang", "scapular_pulls", "flexed_arm_hang", "negatives", "pull_up").map {
                "first_pull_up.$it"
            },
            SkillCatalogue.firstPullUp.gates.map { it.id },
        )
        assertEquals(
            listOf("dead_hang", "dips", "pull_ups", "chest_to_bar", "false_grip_hang", "muscle_up").map {
                "muscle_up.$it"
            },
            SkillCatalogue.muscleUp.gates.map { it.id },
        )
    }

    @Test
    fun everyGateBelongsToItsPathAndEveryPathEndsAtItsGoal() {
        SkillCatalogue.all.forEach { path ->
            assertTrue(path.gates.all { it.id.startsWith("${path.id}.") }, path.id)
            assertEquals("The goal.", path.gates.last().trains, path.id)
            assertTrue(path.about.isNotBlank(), path.id)
        }
    }

    @Test
    fun kitIsWhatTheGatesNeed() {
        assertEquals(emptySet(), SkillTraining.kitFor(checkNotNull(SkillCatalogue.byId("first_push_up"))))
        assertEquals(
            setOf(Equipment.PULL_UP_BAR),
            SkillTraining.kitFor(checkNotNull(SkillCatalogue.byId("front_lever"))),
        )
        assertEquals(setOf(Equipment.DIP_BARS), SkillTraining.kitFor(checkNotNull(SkillCatalogue.byId("first_dip"))))
    }

    @Test
    fun leversCountAsPullingAndPlancheAsPushing() {
        assertEquals(
            setOf(MovementPattern.HORIZONTAL_PULL),
            SkillTraining.patternsOf(checkNotNull(SkillCatalogue.byId("front_lever"))),
        )
        assertEquals(
            setOf(MovementPattern.HORIZONTAL_PUSH),
            SkillTraining.patternsOf(checkNotNull(SkillCatalogue.byId("tuck_planche"))),
        )
    }

    @Test
    fun everyProgramGetsAFirstSessionThatTrainsItsFirstGate() {
        SkillCatalogue.all.forEach { path ->
            val profile = CoachProfile(path.id, PlanStyle.FOCUSED, 3, 45, everything)
            val session = Coach().recommend(AthleteModel.starting(profile, PullUpAnswer.NOT_YET), atDay(0)).session
            val gates = session?.plans?.mapNotNull { it.gateId }.orEmpty()
            assertEquals(listOf(path.gates.first().id), gates.distinct(), "first session for ${path.id}")
        }
    }

    @Test
    fun theEstimatesAreHonestAboutTheHardSkills() {
        val weeks = SkillCatalogue.all.associate { it.id to it.weeks }
        assertTrue(weeks.getValue("front_lever") >= 52)
        assertTrue(weeks.getValue("one_arm_pull_up") > weeks.getValue("ten_pull_ups"))
        assertTrue(SkillCatalogue.all.filter { it.level == SkillLevel.BEGINNER }.all { it.weeks <= 8 })
    }
}
