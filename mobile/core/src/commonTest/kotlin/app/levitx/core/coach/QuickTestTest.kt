package app.levitx.core.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** The optional quick test at setup, and how its results place each ladder. */
class QuickTestTest {
    private val noBox = fullKit - Equipment.BOX

    private fun tested(
        vararg results: Pair<QuickTest, Int>,
        kit: Set<Equipment> = fullKit,
        pullUp: PullUpAnswer = PullUpAnswer.NOT_YET,
    ) = AthleteModel.starting(pullUpProfile(equipment = kit), pullUp, results.toMap())

    @Test
    fun theTestsOfferedFollowTheKit() {
        assertEquals(
            listOf(QuickTest.PUSH_UPS, QuickTest.PIKE_PUSH_UPS, QuickTest.SQUATS),
            QuickTest.forKit(emptySet()),
        )
        assertEquals(
            listOf(QuickTest.PUSH_UPS, QuickTest.PULL_UPS, QuickTest.DIPS, QuickTest.SQUATS),
            QuickTest.forKit(setOf(Equipment.PULL_UP_BAR, Equipment.DIP_BARS)),
        )
    }

    @Test
    fun everyTestIsALadderExercise() {
        QuickTest.entries.forEach { test ->
            val ladder = ExerciseCatalogue.ladder(test.exercise.pattern)
            assert(ladder.rungs.any { rung -> rung.options.any { it.id == test.exerciseId } }) { test.name }
        }
    }

    @Test
    fun aResultInsideTheRangeStartsOnTheTestExercise() {
        val model = tested(QuickTest.PUSH_UPS to 9)
        assertEquals(PUSH_UP_RUNG, model.rung(MovementPattern.HORIZONTAL_PUSH))
        val state = model.exercises.getValue(QuickTest.PUSH_UPS.exerciseId)
        assertEquals(6, state.target, "70% of 9, rounded")
        assertEquals(9, state.best)
        assertEquals(0, state.timesTrained, "the first sessions still check the level")
    }

    @Test
    fun theTopOfTheRangeMovesUpOneRungAndNoMore() {
        assertEquals(PUSH_UP_RUNG + 1, tested(QuickTest.PUSH_UPS to 40).rung(MovementPattern.HORIZONTAL_PUSH))
        assertEquals(BODYWEIGHT_SQUAT_RUNG + 1, tested(QuickTest.SQUATS to 60).rung(MovementPattern.SQUAT))
    }

    @Test
    fun withNothingHarderToDoTheTargetRisesInstead() {
        val model = tested(QuickTest.PUSH_UPS to 25, kit = noBox)
        assertEquals(PUSH_UP_RUNG, model.rung(MovementPattern.HORIZONTAL_PUSH), "the harder push-ups need a box")
        assertEquals(18, model.exercises.getValue(QuickTest.PUSH_UPS.exerciseId).target)
    }

    @Test
    fun aResultUnderTheRangeMovesDownToARungTheKitAllows() {
        assertEquals(PUSH_UP_RUNG - 1, tested(QuickTest.PUSH_UPS to 3).rung(MovementPattern.HORIZONTAL_PUSH))
        assertEquals(
            KNEE_PUSH_UP_RUNG,
            tested(QuickTest.PUSH_UPS to 3, kit = noBox).rung(MovementPattern.HORIZONTAL_PUSH),
            "hands on a box needs a box, so knees",
        )
        assertEquals(PULL_UP_RUNG - 1, tested(QuickTest.PULL_UPS to 2).rung(MovementPattern.VERTICAL_PULL))
    }

    @Test
    fun noneStartsAtTheBottomEvenAfterAYes() {
        assertEquals(PULL_UP_RUNG, tested(pullUp = PullUpAnswer.YES).rung(MovementPattern.VERTICAL_PULL))
        val model = tested(QuickTest.PULL_UPS to 0, pullUp = PullUpAnswer.YES)
        assertEquals(0, model.rung(MovementPattern.VERTICAL_PULL))
        assertNull(model.exercises[QuickTest.PULL_UPS.exerciseId])
    }

    @Test
    fun skippingTheTestChangesNothing() {
        assertEquals(newcomer(), tested())
    }

    @Test
    fun aCountOutsideTheTestsRangeIsRefused() {
        assertFailsWith<IllegalArgumentException> { tested(QuickTest.PUSH_UPS to -1) }
        assertFailsWith<IllegalArgumentException> { tested(QuickTest.PUSH_UPS to QuickTest.PUSH_UPS.maxReps + 1) }
    }

    @Test
    fun thePlacedRungIsWhatTheFirstSessionTrains() {
        val model = tested(QuickTest.PUSH_UPS to 9)
        val session = checkNotNull(Coach().recommend(model, atDay(1)).session)
        val pushes = session.plans.map { it.exercise }.filter { it.pattern == MovementPattern.HORIZONTAL_PUSH }
        val onRung = ExerciseCatalogue.ladder(MovementPattern.HORIZONTAL_PUSH).rungs[PUSH_UP_RUNG].options
        pushes.forEach { assert(it in onRung) { "${it.id} isn't on the placed rung" } }
    }

    private companion object {
        const val KNEE_PUSH_UP_RUNG = 2
        const val PUSH_UP_RUNG = 4
        const val PULL_UP_RUNG = 3
        const val BODYWEIGHT_SQUAT_RUNG = 1
    }
}
