package app.levitx.core.intervals

import app.levitx.core.coach.ExerciseGuides
import app.levitx.core.coach.ExerciseLookup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Interval workouts: the catalogue, the timeline the timer counts down, and the run. */
class IntervalTest {
    private val tabata = checkNotNull(IntervalCatalogue.byId("tabata"))
    private val stretch = checkNotNull(IntervalCatalogue.byId("hips_and_hamstrings"))

    @Test
    fun theCatalogueHasBothKindsAndEveryMoveCanBeFoundAndCoached() {
        assertEquals(5, IntervalCatalogue.all.count { it.kind == IntervalKind.CARDIO })
        assertEquals(4, IntervalCatalogue.all.count { it.kind == IntervalKind.MOBILITY })
        IntervalCatalogue.all.flatMap { it.blocks }.map { it.move }.forEach { move ->
            assertEquals(move, ExerciseLookup.byId(move.id), move.id)
            assertTrue(ExerciseGuides.forExercise(move.id) != null, "${move.id} has cues")
            assertFalse(move.pattern.isStrength, "${move.id} never counts as strength work")
        }
    }

    @Test
    fun everyWorkoutTakesASensibleTime() {
        IntervalCatalogue.all.forEach { assertTrue(it.minutes in 8..30, "${it.id} takes ${it.minutes} min") }
    }

    @Test
    fun theTimelineStartsWithGettingReadyAndEndsOnWork() {
        val steps = IntervalTimeline.of(tabata)
        assertEquals(IntervalPhase.PREPARE, steps.first().phase)
        assertEquals(IntervalPhase.WORK, steps.last().phase)
        // Warm-up 2 rounds, then three blocks of 8.
        assertEquals(2 + 8 * 3, steps.count { it.phase == IntervalPhase.WORK })
        val minuteRests = steps.count { it.phase == IntervalPhase.REST && it.seconds == 60 }
        assertEquals(2, minuteRests, "a minute between the Tabata blocks")
    }

    @Test
    fun oneSidedMovesAreDoneOnBothSidesWithASwitchBetween() {
        val hipFlexor = IntervalTimeline.of(stretch).filter { it.blockIndex == 0 }
        assertEquals(
            listOf(
                IntervalPhase.PREPARE,
                IntervalPhase.WORK,
                IntervalPhase.SWITCH,
                IntervalPhase.WORK,
                IntervalPhase.REST,
            ),
            hipFlexor.take(5).map { it.phase },
        )
        assertEquals(
            listOf(Side.LEFT, Side.RIGHT),
            hipFlexor.filter {
                it.phase == IntervalPhase.WORK
            }.take(2).map { it.side },
        )
    }

    @Test
    fun ticksCarryOverIntoTheNextStepSoNoTimeIsLost() {
        val run = IntervalRun(IntervalTimeline.of(tabata))
        val later = run.tick(IntervalTimeline.PREPARE_SECONDS + 5)
        assertEquals(1, later.index)
        assertEquals(35, later.remaining, "5 seconds into the first 40 second round")
        assertEquals(5, later.worked[1])
        assertEquals(run.totalRemaining - IntervalTimeline.PREPARE_SECONDS - 5, later.totalRemaining)
    }

    @Test
    fun aPausedRunDoesNotMove() {
        val paused = IntervalRun(IntervalTimeline.of(tabata)).togglePause()
        assertEquals(paused, paused.tick(30))
        assertFalse(paused.togglePause().paused)
    }

    @Test
    fun runningToTheEndLogsEveryRoundOfEveryMove() {
        val steps = IntervalTimeline.of(tabata)
        val done = IntervalRun(steps).tick(steps.sumOf { it.seconds })
        assertTrue(done.isFinished)
        val sessions = done.sessions(0L, 1_000L)
        assertEquals(
            listOf("cardio.jumping_jacks", "cardio.burpees", "cardio.high_knees", "cardio.mountain_climbers"),
            sessions.map {
                it.gateId
            },
        )
        assertEquals(List(8) { 20 }, sessions[1].sets.map { it.value })
    }

    @Test
    fun skippingKeepsWhatWasDoneAndEndingEarlyLogsOnlyThat() {
        val run = IntervalRun(IntervalTimeline.of(tabata)).skip().tick(12).skip()
        assertEquals(listOf(12), run.sessions(0L, 1L).single().sets.map { it.value })
        assertTrue(
            IntervalRun(IntervalTimeline.of(tabata)).skip().sessions(0L, 1L).isEmpty(),
            "no work, nothing logged",
        )
    }

    @Test
    fun aBlockRefusesAStrengthExerciseOrSillyNumbers() {
        val pushUp = checkNotNull(ExerciseLookup.byId("horizontal_push.push_up"))
        assertFailsWith<IllegalArgumentException> { IntervalBlock(pushUp, 30, 30, 3) }
        assertFailsWith<IllegalArgumentException> { IntervalBlock(burpees, 0, 30, 3) }
        assertFailsWith<IllegalArgumentException> { IntervalBlock(burpees, 30, 30, 0) }
    }

    @Test
    fun quietAndBeginnerWorkoutsLeaveOutJumping() {
        assertFalse(tabata.isQuiet)
        assertTrue(stretch.isQuiet)
        val quietCardio = IntervalCatalogue.all.filter { it.kind == IntervalKind.CARDIO && it.isQuiet }
        assertEquals(listOf("quiet_cardio"), quietCardio.map { it.id }, "one cardio workout for a flat")
        assertTrue(IntervalCatalogue.all.filter { it.forBeginners }.all { it.isQuiet })
        assertTrue(IntervalCatalogue.all.filter { it.kind == IntervalKind.MOBILITY }.all { it.forBeginners })
    }

    @Test
    fun aWorkoutForBeginnersCantJump() {
        assertFailsWith<IllegalArgumentException> {
            IntervalWorkout(
                "x",
                "X",
                IntervalKind.CARDIO,
                "",
                listOf(IntervalBlock(burpees, 30, 30, 2)),
                forBeginners = true,
            )
        }
    }
}
