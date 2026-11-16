package app.levitx.core.coach

import app.levitx.core.session.PlannedSet
import app.levitx.core.session.SetLoad
import app.levitx.core.skills.SkillCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkoutRunTest {
    private val pushUp = exercise("horizontal_push.push_up")
    private val squat = exercise("squat.bodyweight")
    private val session = sessionOf(hardPlan(pushUp, 8).trimmedTo(2), hardPlan(squat, 12).trimmedTo(2))
    private val start = WorkoutRun(id = "w1", session = session, startedAtEpochMillis = 1_000)

    @Test
    fun startsOnTheFirstSetOfTheFirstExercise() {
        assertEquals(pushUp, start.current?.exercise)
        assertEquals(8, start.nextSet?.target)
        assertFalse(start.hasStarted || start.isResting || start.isComplete)
    }

    @Test
    fun loggingASetStartsRestAndKeepsTheLoad() {
        val loaded = start.withLoad(SetLoad(addedKg = 5.0))
        val after = loaded.logSet(8, repsInReserve = 2, nowElapsedMillis = 10_000)
        assertEquals(10_000 + 120_000L, after.restEndsAtElapsedMillis)
        assertEquals(5.0, after.logged[0].single().addedKg)
        assertEquals(SetLoad(addedKg = 5.0), after.load)
        assertEquals(1, after.nextSetIndex)
    }

    @Test
    fun nothingIsLoggedDuringRest() {
        val resting = start.logSet(8, 2, 0)
        assertEquals(resting, resting.logSet(8, 2, 1))
    }

    @Test
    fun finishingAnExerciseMovesOnAndResetsTheLoad() {
        val after = start.withLoad(SetLoad(addedKg = 5.0)).logSet(8, 2, 0).endRest().logSet(7, 1, 1)
        assertEquals(squat, after.current?.exercise)
        assertEquals(0, after.nextSetIndex)
        assertEquals(SetLoad.None, after.load)
        assertTrue(after.isResting, "rest between exercises too")
    }

    @Test
    fun theLastSetEndsTheWorkoutWithoutRest() {
        val done = listOf(8, 8, 12, 12).fold(start) { run, reps -> run.endRest().logSet(reps, 2, 0) }
        assertTrue(done.isComplete)
        assertNull(done.restEndsAtElapsedMillis)
        assertNull(done.current)
        assertEquals(done, done.logSet(5, 2, 0), "nothing more can be logged")
    }

    @Test
    fun skippingAnUntouchedExerciseIsRemembered() {
        val skipped = start.skipExercise()
        assertEquals(setOf(0), skipped.skipped)
        assertEquals(squat, skipped.current?.exercise)
        val result = assertNotNull(skipped.logSet(12, 2, 0).result(finishedAtEpochMillis = 5_000))
        assertTrue(result.results.first().skipped)
    }

    @Test
    fun endingAnExerciseEarlyKeepsWhatWasDone() {
        val early = start.logSet(8, 2, 0).skipExercise()
        assertTrue(early.skipped.isEmpty())
        assertFalse(early.isResting)
        val result = assertNotNull(early.result(5_000)).results.single()
        assertEquals(1, result.sets.size)
        assertFalse(result.skipped)
    }

    @Test
    fun stoppingEarlyLeavesExercisesNotReachedOutOfTheResult() {
        val result = assertNotNull(start.logSet(8, 2, 0).result(5_000))
        assertEquals(listOf(pushUp), result.results.map { it.plan.exercise })
    }

    @Test
    fun noResultBeforeTheFirstSet() {
        assertNull(start.result(5_000))
        assertNull(start.skipExercise().result(5_000), "skipping alone isn't a workout")
    }

    @Test
    fun theResultCarriesTheChoiceAndNeverFinishesBeforeItStarted() {
        val lighter = start.copy(choice = SuggestionChoice.LIGHTER).logSet(8, 2, 0)
        val result = assertNotNull(lighter.result(finishedAtEpochMillis = 0))
        assertEquals(SuggestionChoice.LIGHTER, result.choice)
        assertEquals(1_000, result.finishedAtEpochMillis)
    }

    @Test
    fun aRunTheCoachRecordsLikeAnyOtherSession() {
        val coach = Coach()
        val model = newcomer()
        val planned = assertNotNull(coach.recommend(model, atDay(0)).session)
        var run = WorkoutRun("w2", planned, atDay(0))
        while (!run.isComplete) {
            run = run.endRest().logSet(assertNotNull(run.nextSet).target, 2, 0)
        }
        val after = coach.record(model, assertNotNull(run.result(atDay(0) + MILLIS_PER_HOUR)))
        assertEquals(planned.hardSets, after.history.single().hardSets)
    }

    @Test
    fun badStatesAreRefused() {
        assertFailsWith<IllegalArgumentException> { start.copy(id = " ") }
        assertFailsWith<IllegalArgumentException> { start.copy(exerciseIndex = 3) }
        assertFailsWith<IllegalArgumentException> { start.copy(logged = listOf(emptyList())) }
    }

    @Test
    fun everyPlannedExerciseCanBeFoundById() {
        val planned = assertNotNull(Coach().recommend(newcomer(), atDay(0)).session)
        planned.plans.forEach { assertEquals(it.exercise, ExerciseLookup.byId(it.exercise.id)) }
        assertNull(ExerciseLookup.byId("not.an.exercise"))
    }

    @Test
    fun restLongerThanAnyInThePlanIsEndedAfterARestart() {
        val resting = start.logSet(8, 2, nowElapsedMillis = 50_000_000)
        // After a restart the elapsed clock is back near zero, so this rest looks hours long.
        assertNull(resting.withSaneRest(nowElapsedMillis = 5_000).restEndsAtElapsedMillis)
        // Without a restart the rest is left alone.
        assertEquals(resting, resting.withSaneRest(nowElapsedMillis = 50_010_000))
    }

    @Test
    fun theResultReportsTheBestStrictSetPerGate() {
        val gate = checkNotNull(newcomer().currentGate())
        val hang = hardPlan(checkNotNull(SkillTraining.exerciseFor(gate)), 20).copy(gateId = gate.id)
        val run =
            WorkoutRun("w2", sessionOf(hang.trimmedTo(2), hardPlan(squat, 12).trimmedTo(1)), startedAtEpochMillis = 0)
        val done = run.logSet(20, null, 0).endRest().logSet(25, null, 0).endRest().logSet(12, 2, 0)
        assertEquals(mapOf(gate.id to 25), done.result(finishedAtEpochMillis = 1)?.gateBests)
        assertEquals(emptyMap(), start.logSet(8, 2, 0).result(finishedAtEpochMillis = 1)?.gateBests)
        // Only a step past the previous best is a new best.
        val result = checkNotNull(done.result(finishedAtEpochMillis = 1))
        assertEquals(mapOf(gate.id to 25), result.newGateBests(mapOf(gate.id to 24)))
        assertEquals(emptyMap(), result.newGateBests(mapOf(gate.id to 25)))
    }

    @Test
    fun restCanBeMadeLongerOrShorterButNeverEndsInThePast() {
        val resting = start.logSet(8, 2, nowElapsedMillis = 0)
        assertEquals(135_000L, resting.adjustRest(15_000, nowElapsedMillis = 10_000).restEndsAtElapsedMillis)
        assertEquals(105_000L, resting.adjustRest(-15_000, nowElapsedMillis = 10_000).restEndsAtElapsedMillis)
        // Taking off more than is left ends it now, still resting, so the screen shows it as done.
        assertEquals(110_000L, resting.adjustRest(-200_000, nowElapsedMillis = 110_000).restEndsAtElapsedMillis)
        assertEquals(start, start.adjustRest(15_000, nowElapsedMillis = 0), "nothing to adjust before a set")
    }

    @Test
    fun undoTakesBackTheSetJustLoggedWithItsLoad() {
        val loaded = start.withLoad(SetLoad(addedKg = 5.0))
        val resting = loaded.logSet(8, 2, 0)
        assertEquals(loaded, resting.undoLastSet())
    }

    @Test
    fun undoAfterTheLastSetOfAnExerciseGoesBackToThatExercise() {
        val first = start.logSet(8, 2, 0).endRest()
        val movedOn = first.logSet(7, 1, 1)
        assertEquals(squat, movedOn.current?.exercise)
        val undone = movedOn.undoLastSet()
        assertEquals(pushUp, undone.current?.exercise)
        assertEquals(1, undone.nextSetIndex)
        assertFalse(undone.isResting)
    }

    @Test
    fun undoOnlyWorksDuringTheRestThatSetStarted() {
        val afterRest = start.logSet(8, 2, 0).endRest()
        assertEquals(afterRest, afterRest.undoLastSet())
        assertEquals(start, start.undoLastSet())
    }

    private val gate = checkNotNull(SkillCatalogue.gateById("muscle_up.pull_ups"))
    private val attempt = ExercisePlan(
        exercise = checkNotNull(SkillTraining.exerciseFor(gate)),
        sets = listOf(PlannedSet(10, isBestEffort = true)) + List(3) { PlannedSet(6, isBestEffort = false) },
        restSeconds = 120,
        repsInReserve = 2,
        intensity = Intensity.HARD,
        gateId = gate.id,
        isGateAttempt = true,
    )

    @Test
    fun puttingOffAnAttemptMovesItToTheEndAsAnOrdinaryBestEffort() {
        val run = WorkoutRun("w", sessionOf(attempt), startedAtEpochMillis = 0)
        val plan = checkNotNull(run.deferAttempt().current)
        assertFalse(plan.isGateAttempt)
        assertEquals(6, plan.sets.first().target)
        assertEquals(PlannedSet(10, isBestEffort = true), plan.sets.last())
    }

    @Test
    fun anAttemptCantBePutOffOnceStarted() {
        val started = WorkoutRun("w", sessionOf(attempt), startedAtEpochMillis = 0).logSet(9, null, 0)
        assertEquals(started, started.deferAttempt())
        assertEquals(start, start.deferAttempt())
    }

    @Test
    fun lightWorkNeverCarriesAnAttempt() {
        val light = attempt.lightened()
        assertFalse(light.isGateAttempt)
        assertTrue(light.sets.none { it.isBestEffort })
    }
}
