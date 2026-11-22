package app.levitx.core.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Swapping an exercise for another on the same rung, and the coach remembering it. */
class SwapTest {
    private val coach = Coach()
    private val splitSquat = exercise("squat.split")
    private val reverseLunge = exercise("squat.reverse_lunge")
    private val stepUp = exercise("squat.step_up")
    private val bulgarian = exercise("squat.bulgarian")

    /** A newcomer on the split squat rung, in block [blockIndex], with [equipment]. */
    private fun onSplitSquats(blockIndex: Int = 0, equipment: Set<Equipment> = fullKit): AthleteModel {
        val rung = ExerciseCatalogue.ladder(MovementPattern.SQUAT).rungs.indexOfFirst { splitSquat in it.options }
        return newcomer(pullUpProfile(equipment = equipment)).copy(
            rungs = mapOf(MovementPattern.SQUAT to rung),
            block = TrainingBlock(index = blockIndex),
        )
    }

    @Test
    fun theOptionsAreTheRungsExercisesTheKitAllows() {
        assertEquals(listOf(splitSquat, reverseLunge), onSplitSquats().swapOptions(MovementPattern.SQUAT))
    }

    @Test
    fun kitTheyDontHaveIsNeverOffered() {
        val noBox = onSplitSquats().copy(rungs = mapOf(MovementPattern.SQUAT to rungOf(bulgarian)))
            .let { it.copy(profile = it.profile.copy(equipment = setOf(Equipment.PULL_UP_BAR))) }
        assertEquals(listOf(bulgarian), noBox.swapOptions(MovementPattern.SQUAT))
        assertTrue(stepUp !in noBox.swapOptions(MovementPattern.SQUAT))
    }

    @Test
    fun aChoiceWinsOverTheBlocksRotation() {
        // Block 1 would rotate to the reverse lunge; the person chose split squats.
        val model = onSplitSquats(blockIndex = 1)
        assertEquals(reverseLunge, model.currentExercise(MovementPattern.SQUAT))
        val chosen = model.withPreferred(splitSquat)
        assertEquals(splitSquat, chosen.currentExercise(MovementPattern.SQUAT))
        assertEquals(splitSquat, chosen.copy(block = TrainingBlock(index = 2)).currentExercise(MovementPattern.SQUAT))
    }

    @Test
    fun theSessionUsesTheChoice() {
        val chosen = onSplitSquats().withPreferred(reverseLunge)
        val session = coach.recommend(chosen, atDay(0)).session
        val squats = session?.plans?.map { it.exercise }?.filter { it.pattern == MovementPattern.SQUAT }.orEmpty()
        assertTrue(squats.isNotEmpty(), "the session trains squats")
        assertTrue(reverseLunge in squats && splitSquat !in squats)
    }

    @Test
    fun movingUpARungLetsTheCoachChooseAgain() {
        val chosen = onSplitSquats().withPreferred(reverseLunge)
        val movedUp = chosen.copy(rungs = mapOf(MovementPattern.SQUAT to rungOf(bulgarian)))
        assertEquals(bulgarian, movedUp.currentExercise(MovementPattern.SQUAT))
    }

    @Test
    fun anExerciseThatIsntAnOptionChangesNothing() {
        val model = onSplitSquats()
        assertSame(model, model.withPreferred(bulgarian))
        assertSame(model, model.withPreferred(exercise("vertical_pull.pull_up")))
    }

    @Test
    fun losingTheKitFallsBackToWhatTheyCanDo() {
        val onBulgarian = onSplitSquats().copy(rungs = mapOf(MovementPattern.SQUAT to rungOf(bulgarian)))
        val chosen = onBulgarian.withPreferred(stepUp)
        assertEquals(stepUp, chosen.currentExercise(MovementPattern.SQUAT))
        val noBox = chosen.copy(profile = chosen.profile.copy(equipment = setOf(Equipment.PULL_UP_BAR)))
        assertEquals(bulgarian, noBox.currentExercise(MovementPattern.SQUAT))
    }

    @Test
    fun cardioAndMobilityHaveNothingToSwap() {
        assertTrue(onSplitSquats().swapOptions(MovementPattern.CARDIO).isEmpty())
    }

    private fun rungOf(exercise: Exercise): Int =
        ExerciseCatalogue.ladder(exercise.pattern).rungs.indexOfFirst { exercise in it.options }
}
