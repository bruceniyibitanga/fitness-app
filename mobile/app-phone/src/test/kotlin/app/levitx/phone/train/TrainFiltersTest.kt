package app.levitx.phone.train

import app.levitx.core.coach.Coach
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.SkillTraining
import app.levitx.core.coach.WorkoutFocus
import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalKind
import app.levitx.core.skills.SkillLevel
import app.levitx.testing.newcomer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainFiltersTest {
    // Someone with a pull-up bar, so My kit and No kit differ.
    private val model = newcomer()
    private val ready = TrainUiState.Ready(
        programs = programsFor(model),
        workouts = workoutsFor(model, Coach(), nowEpochMillis = 0L),
        owned = model.profile.equipment,
    )

    private fun lists(chip: TrainChip = TrainChip.ALL, vararg filters: TrainFilter) =
        trainLists(ready, chip, filters.toSet())

    @Test
    fun allWithNoFiltersShowsEverything() {
        val all = lists()
        assertEquals(ready.programs, all.programs)
        assertEquals(ready.workouts, all.workouts)
        assertEquals(IntervalCatalogue.all, all.intervals)
    }

    @Test
    fun everyWorkoutFocusHasAChip() {
        assertEquals(WorkoutFocus.entries.toSet(), TrainChip.entries.mapNotNull { it.focus }.toSet())
    }

    @Test
    fun aBodyChipShowsItsWorkoutAndTheProgramsButNoCardio() {
        val pull = lists(TrainChip.PULL)
        assertEquals(listOf(WorkoutFocus.PULL), pull.workouts.map { it.focus })
        assertEquals(ready.programs, pull.programs)
        assertTrue(pull.intervals.isEmpty())
    }

    @Test
    fun cardioAndMobilityChipsShowOnlyThoseWorkouts() {
        val cardio = lists(TrainChip.CARDIO)
        assertTrue(cardio.programs.isEmpty() && cardio.workouts.isEmpty())
        assertTrue(cardio.intervals.isNotEmpty() && cardio.intervals.all { it.kind == IntervalKind.CARDIO })
        assertTrue(lists(TrainChip.MOBILITY).intervals.all { it.kind == IntervalKind.MOBILITY })
    }

    @Test
    fun kitFiltersReplaceEachOther() {
        val noKit = setOf(TrainFilter.NO_KIT, TrainFilter.QUIET)
        assertEquals(setOf(TrainFilter.MY_KIT, TrainFilter.QUIET), noKit.toggled(TrainFilter.MY_KIT))
        assertEquals(setOf(TrainFilter.QUIET), noKit.toggled(TrainFilter.NO_KIT))
    }

    @Test
    fun noKitKeepsOnlyWhatNeedsNothing() {
        val noKit = lists(filters = arrayOf(TrainFilter.NO_KIT))
        assertTrue(noKit.programs.all { SkillTraining.kitFor(it.path).isEmpty() })
        assertTrue(noKit.workouts.all { workout -> workout.session!!.plans.all { it.exercise.equipment.isEmpty() } })
        assertTrue(noKit.intervals.none { Equipment.JUMP_ROPE in it.equipment })
    }

    @Test
    fun myKitKeepsProgramsTheBarIsEnoughFor() {
        val mine = lists(filters = arrayOf(TrainFilter.MY_KIT))
        assertTrue(mine.programs.any { it.path.id == "first_pull_up" })
        assertTrue("rings aren't owned", mine.programs.none { Equipment.RINGS in SkillTraining.kitFor(it.path) })
        assertTrue("no rope", mine.intervals.none { Equipment.JUMP_ROPE in it.equipment })
        assertEquals("single workouts are built from the person's kit", ready.workouts, mine.workouts)
    }

    @Test
    fun underTwentyMinutesHidesProgramsAndLongWorkouts() {
        val short = lists(filters = arrayOf(TrainFilter.SHORT))
        assertTrue(short.programs.isEmpty())
        assertTrue(short.workouts.all { it.session!!.estimatedMinutes < SHORT_MINUTES })
        assertTrue(short.intervals.all { it.minutes < SHORT_MINUTES })
    }

    @Test
    fun quietAndBeginnerLeaveOutJumpingAndHarderThings() {
        val quiet = lists(TrainChip.CARDIO, TrainFilter.QUIET)
        assertEquals(listOf("quiet_cardio"), quiet.intervals.map { it.id })
        val beginner = lists(filters = arrayOf(TrainFilter.BEGINNER))
        assertTrue(beginner.programs.isNotEmpty() && beginner.programs.all { it.path.level == SkillLevel.BEGINNER })
        assertTrue(beginner.intervals.all { it.forBeginners })
    }

    @Test
    fun aListWithNothingLeftIsEmpty() {
        val nothing = TrainUiState.Ready(programs = emptyList(), workouts = emptyList())
        assertTrue(trainLists(nothing, TrainChip.PULL, emptySet()).isEmpty)
        assertTrue(!lists(TrainChip.MOBILITY, TrainFilter.QUIET).isEmpty)
    }
}
