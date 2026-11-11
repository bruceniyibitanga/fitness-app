package app.levitx.core.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VolumeTargetsTest {
    @Test
    fun theGoalPatternGetsTheMostInAnAllRoundPlan() {
        val targets = VolumeTargets.weekly(pullUpProfile())
        assertEquals(CoachTuning.GOAL_PATTERN_WEEKLY_SETS, targets[MovementPattern.VERTICAL_PULL])
        assertEquals(CoachTuning.ALL_ROUND_WEEKLY_SETS, targets[MovementPattern.SQUAT])
        assertEquals(MovementPattern.entries.toSet(), targets.keys)
    }

    @Test
    fun aFocusedPlanDoesMoreOfTheGoalAndOnlyHoldsTheRestSteady() {
        val targets = VolumeTargets.weekly(pullUpProfile(style = PlanStyle.FOCUSED))
        assertEquals(CoachTuning.FOCUSED_GOAL_WEEKLY_SETS, targets[MovementPattern.VERTICAL_PULL])
        assertEquals(CoachTuning.MAINTENANCE_WEEKLY_SETS, targets[MovementPattern.SQUAT])
    }

    @Test
    fun rowsSupportAPullUpGoalSoAFocusedPlanKeepsThem() {
        assertEquals(PatternRole.GOAL, VolumeTargets.roleOf(MovementPattern.VERTICAL_PULL, pullUpProfile()))
        assertEquals(PatternRole.SUPPORT, VolumeTargets.roleOf(MovementPattern.HORIZONTAL_PULL, pullUpProfile()))
        assertEquals(PatternRole.OTHER, VolumeTargets.roleOf(MovementPattern.SQUAT, pullUpProfile()))
        val targets = VolumeTargets.weekly(pullUpProfile(style = PlanStyle.FOCUSED))
        assertEquals(CoachTuning.ALL_ROUND_WEEKLY_SETS, targets[MovementPattern.HORIZONTAL_PULL])
    }

    @Test
    fun withNoGoalEveryPatternGetsTheSame() {
        val targets = VolumeTargets.weekly(CoachProfile(goalPathId = null, equipment = fullKit))
        assertEquals(setOf(CoachTuning.ALL_ROUND_WEEKLY_SETS), targets.values.toSet())
    }

    @Test
    fun targetsShrinkTogetherWhenTheWeekIsShort() {
        val roomy = VolumeTargets.weekly(pullUpProfile())
        val tight = VolumeTargets.weekly(pullUpProfile(sessionMinutes = 20))
        assertTrue(tight.values.sum() < roomy.values.sum())
        val ratios = MovementPattern.entries.map { tight.getValue(it) / roomy.getValue(it) }
        assertEquals(ratios.first(), ratios.last(), absoluteTolerance = 1e-9)
        // Three 20-minute sessions leave 15 minutes of work each: 18 sets a week.
        assertEquals(18.0, tight.values.sum(), absoluteTolerance = 1e-9)
    }

    @Test
    fun patternsWithoutTheKitHaveNoTarget() {
        val targets = VolumeTargets.weekly(pullUpProfile(equipment = emptySet()))
        assertTrue(MovementPattern.VERTICAL_PULL !in targets)
        assertTrue(MovementPattern.HORIZONTAL_PULL !in targets)
        assertTrue(MovementPattern.SQUAT in targets)
    }
}
