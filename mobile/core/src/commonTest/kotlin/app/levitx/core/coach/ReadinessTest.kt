package app.levitx.core.coach

import app.levitx.core.session.LoggedSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadinessTest {
    private val coach = Coach()
    private val pushUp = exercise("horizontal_push.push_up")
    private val session = sessionOf(hardPlan(pushUp, 8))

    private fun settled(): AthleteModel = newcomer().copy(
        rungs = mapOf(MovementPattern.HORIZONTAL_PUSH to 3),
        exercises = mapOf(pushUp.id to ExerciseState(target = 8, timesTrained = 5)),
    )

    /** Push-up sessions on days 0, 2, 4 and so on, with these reps on every set. */
    private fun AthleteModel.sessions(
        vararg reps: Int,
        effort: Effort = Effort.RIGHT,
        choice: SuggestionChoice? = null,
    ): AthleteModel = reps.foldIndexed(this) { index, model, value ->
        coach.record(model, finished(index * 2, done(hardPlan(pushUp, 8), value, effort), choice = choice))
    }

    private fun AthleteModel.assess(day: Int): Suggestion? = ReadinessAssessor.assess(session, contextOn(day, this))

    @Test
    fun aSteadyPersonGetsNoSuggestion() {
        assertNull(settled().sessions(8, 8, 8).assess(day = 6))
    }

    @Test
    fun fallingResultsAloneAreNotEnough() {
        val model = settled().sessions(8, 7, 6)
        assertTrue(PerformanceTrend(since = null).isDropping(model.exercises.getValue(pushUp.id)))
        assertNull(model.assess(day = 6))
    }

    @Test
    fun aSmallWobbleOrOneBadDayIsNotADrop() {
        val trend = PerformanceTrend(since = null)
        assertTrue(!trend.isDropping(settled().sessions(8, 8, 7).exercises.getValue(pushUp.id)))
        assertTrue(!trend.isDropping(settled().sessions(8, 8, 5).exercises.getValue(pushUp.id)))
    }

    @Test
    fun aBestEffortSetDoesNotMakeTheNextSessionLookLikeADrop() {
        // Two calibration sessions with a big last set, then plain work sets at the same target.
        val calibrating = hardPlan(pushUp, 8, bestEffort = true)
        val big = calibrating.sets.map { LoggedSet(if (it.isBestEffort) 14 else 8, 2, it.isBestEffort) }
        val model = listOf(0, 2).fold(settled()) { model, day ->
            coach.record(model, finished(day, ExerciseResult(calibrating, big)))
        }
        val after = coach.record(model, finished(4, done(hardPlan(pushUp, 8), 8)))
        assertEquals(listOf(8, 8, 8), PerformanceTrend(since = null).recentTops(after.exercises.getValue(pushUp.id)))
    }

    @Test
    fun takingTheEasierDayAnswersTheSignalsThatLedToIt() {
        val worn = settled().sessions(8, 6, 5, effort = Effort.HARD)
        assertNotNull(worn.assess(day = 6))
        val lighter = sessionOf(hardPlan(pushUp, 8).lightened())
        val eased = coach.record(worn, finished(6, done(lighter.plans.single(), 6), choice = SuggestionChoice.LIGHTER))
        assertNull(eased.assess(day = 8), "the same old results don't ask again")
        assertNull(coach.recordRest(worn, atDay(6)).assess(day = 8))

        val stillDown = coach.record(eased, finished(8, done(hardPlan(pushUp, 8), 5, Effort.HARD)))
        assertNotNull(stillDown.assess(day = 10), "a fresh result that is still down does")
    }

    @Test
    fun carryingOnLeavesTheSignalsStanding() {
        val worn = settled().sessions(8, 6, 5, effort = Effort.HARD)
        val kept = coach.record(
            worn,
            finished(6, done(hardPlan(pushUp, 8), 4, Effort.HARD), choice = SuggestionChoice.KEEP),
        )
        assertNotNull(kept.assess(day = 8))
    }

    @Test
    fun fallingResultsThatAlsoFeltHardBringASuggestion() {
        val suggestion = assertNotNull(settled().sessions(8, 6, 5, effort = Effort.HARD).assess(day = 6))
        assertEquals(SuggestionChoice.LIGHTER, suggestion.recommended)
        assertEquals(
            listOf<ReadinessSignal>(
                ReadinessSignal.PerformanceDrop(pushUp.name, listOf(8, 6, 5)),
                ReadinessSignal.FeltHard(pushUp.name),
            ),
            suggestion.signals,
        )
        assertTrue(suggestion.lighter.isLight)
    }

    @Test
    fun signalsAboutExercisesNotInTodaysSessionDoNotCount() {
        val model = settled().sessions(8, 6, 5, effort = Effort.HARD)
        val legs = sessionOf(hardPlan(exercise("squat.bodyweight"), 12))
        assertNull(ReadinessAssessor.assess(legs, contextOn(6, model)))
    }

    @Test
    fun restIsSuggestedWhenTheLastSessionWasWithinADay() {
        val model = settled().sessions(8, 6, 5, effort = Effort.HARD)
        val nextMorning = CoachContext(model, atDay(4) + 14 * MILLIS_PER_HOUR)
        assertEquals(SuggestionChoice.REST, ReadinessAssessor.assess(session, nextMorning)?.recommended)
    }

    @Test
    fun twoWeeksAwayIsEnoughAlone() {
        val model = settled().sessions(8)
        // The session finished an hour into day 0, so day 15 is the first with 14 whole days since.
        val suggestion = assertNotNull(model.assess(day = CoachTuning.COMEBACK_AFTER_DAYS + 1))
        assertEquals(
            listOf<ReadinessSignal>(ReadinessSignal.LongGap(CoachTuning.COMEBACK_AFTER_DAYS)),
            suggestion.signals,
        )
        assertNull(model.assess(day = CoachTuning.COMEBACK_AFTER_DAYS))
    }

    @Test
    fun theLighterWeekIsOfferedNotImposed() {
        val model = settled().copy(block = TrainingBlock(lighterSessionsLeft = 2))
        val suggestion = assertNotNull(model.assess(day = 0))
        assertIs<ReadinessSignal.LighterWeek>(suggestion.signals.single())
        assertEquals(SuggestionChoice.LIGHTER, suggestion.recommended)
    }

    @Test
    fun aLightSessionIsNeverMadeLighter() {
        val model = settled().sessions(8, 6, 5, effort = Effort.HARD)
        assertNull(ReadinessAssessor.assess(session.lightened(), contextOn(6, model)))
    }

    @Test
    fun someoneWhoKeepsGoingAndDoesWellIsAskedLessOften() {
        val kept =
            List(CoachTuning.KEPT_SUGGESTIONS_TO_RAISE_BAR) {
                SuggestionOutcome(SuggestionChoice.KEEP, wentWell = true)
            }
        val model = settled().sessions(8, 6, 5, effort = Effort.HARD).copy(suggestionOutcomes = kept)
        assertNull(model.assess(day = 6), "two signals no longer reach the raised bar")

        val struggled = kept.dropLast(1) + SuggestionOutcome(SuggestionChoice.KEEP, wentWell = false)
        assertNotNull(model.copy(suggestionOutcomes = struggled).assess(day = 6))
    }

    @Test
    fun theChoiceIsRememberedWithHowItWent() {
        val kept = settled().sessions(8, choice = SuggestionChoice.KEEP)
        assertEquals(listOf(SuggestionOutcome(SuggestionChoice.KEEP, wentWell = true)), kept.suggestionOutcomes)
        val missed = settled().sessions(5, choice = SuggestionChoice.KEEP)
        assertEquals(listOf(SuggestionOutcome(SuggestionChoice.KEEP, wentWell = false)), missed.suggestionOutcomes)
        assertEquals(SuggestionChoice.REST, coach.recordRest(settled(), atDay(0)).suggestionOutcomes.single().choice)
    }
}
