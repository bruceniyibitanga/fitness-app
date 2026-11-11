package app.levitx.core.coach

import kotlin.math.abs
import kotlin.math.roundToInt

private const val PERCENT = 100
private const val TENTHS = 10

/**
 * The coach's words, in English. Phone and watch both read from here, so they
 * say the same thing. Every sentence is filled from what the engine did, never
 * written freehand, so it can't claim something that didn't happen.
 */
public object CoachPhrases {
    /** One plain sentence for [reason]. */
    public fun describe(reason: CoachReason): String = when (reason) {
        is CoachReason.Progress -> progress(reason)
        is CoachReason.Choice -> choice(reason)
        is CoachReason.Safety -> safety(reason)
    }

    private fun progress(reason: CoachReason.Progress): String = when (reason) {
        CoachReason.StillLearning -> "Still learning your numbers. Targets are on the easy side for now."

        is CoachReason.TargetRaised -> "${reason.exerciseName}: target up. You hit every set last time."

        is CoachReason.MovedUp -> "${reason.exerciseName}: a step up. You topped out the last one."

        is CoachReason.EasedBack ->
            "${reason.exerciseName}: eased back a step after two sessions short of target."

        is CoachReason.NewThisBlock ->
            "${reason.exerciseName}: new this block. Same muscles, a fresh challenge."
    }

    private fun choice(reason: CoachReason.Choice): String = when (reason) {
        is CoachReason.GoalFirst -> "Your goal comes first today: ${reason.skillName}."

        is CoachReason.ClosingGap ->
            "More ${noun(reason.pattern)} today: you're ${sets(reason.setsShort)} short this week."

        is CoachReason.FreshestFirst -> "Leading with ${noun(reason.pattern)}: it's what's most rested."

        is CoachReason.FitsYourTime -> "About ${reason.minutes} minutes, to fit the time you have."

        CoachReason.YourKindOfSession -> "Built from the exercises you stick with."

        CoachReason.Variety -> "A different mix from last time, same goal."

        is CoachReason.NeedsKit ->
            "${reason.skillName} work needs ${kitList(reason.equipment)}. Add it under Body to start it."
    }

    private fun safety(reason: CoachReason.Safety): String = when (reason) {
        is CoachReason.Recovering ->
            "Going easy on ${noun(reason.pattern)} today: it needs another day after your last session."

        is CoachReason.VolumeCapped ->
            "Going easy on ${noun(reason.pattern)}: you're at this week's limit while your tendons catch up."

        is CoachReason.WorkingAround -> "Working around your ${noun(reason.area)} today."

        CoachReason.HoldingSteady -> "Holding your targets steady while your weight is dropping fast."

        CoachReason.RestDay -> "Nothing to train safely today. Rest, and get that pain looked at."
    }

    /** One plain sentence for [signal], for the screen that offers an easier day. */
    public fun describe(signal: ReadinessSignal): String = when (signal) {
        is ReadinessSignal.PerformanceDrop ->
            "${signal.exerciseName}: ${signal.recentTops.joinToString(" → ")} over your last sessions."

        is ReadinessSignal.FeltHard -> "${signal.exerciseName} felt hard two sessions running."

        is ReadinessSignal.FastWeightLoss ->
            "Your weight is down about ${percent(signal.weeklyChange)}% a week."

        is ReadinessSignal.LongGap -> "It's been ${signal.days} days since your last session."

        ReadinessSignal.LighterWeek -> "This is your block's lighter week."
    }

    /** What to call [pattern] mid-sentence. */
    public fun noun(pattern: MovementPattern): String = when (pattern) {
        MovementPattern.VERTICAL_PULL -> "pulling"
        MovementPattern.HORIZONTAL_PULL -> "rowing"
        MovementPattern.VERTICAL_PUSH -> "overhead pressing"
        MovementPattern.HORIZONTAL_PUSH -> "push-up work"
        MovementPattern.SQUAT -> "squatting"
        MovementPattern.HINGE -> "hip work"
        MovementPattern.CORE -> "core work"
    }

    /** What to call [area] mid-sentence. */
    public fun noun(area: BodyArea): String = when (area) {
        BodyArea.WRIST -> "wrist"
        BodyArea.ELBOW -> "elbow"
        BodyArea.SHOULDER -> "shoulder"
        BodyArea.LOWER_BACK -> "lower back"
        BodyArea.HIP -> "hip"
        BodyArea.KNEE -> "knee"
        BodyArea.ANKLE -> "ankle"
    }

    /** What to call [equipment] mid-sentence. */
    public fun noun(equipment: Equipment): String = when (equipment) {
        Equipment.PULL_UP_BAR -> "a pull-up bar"
        Equipment.LOW_BAR -> "a low bar"
        Equipment.DIP_BARS -> "dip bars"
        Equipment.RINGS -> "rings"
        Equipment.BAND -> "a resistance band"
        Equipment.BOX -> "a box or step"
    }

    private fun sets(count: Int): String = if (count == 1) "1 set" else "$count sets"

    /** A weekly change as a percentage with one decimal, without its sign. */
    private fun percent(fraction: Double): String {
        val tenths = (abs(fraction) * PERCENT * TENTHS).roundToInt()
        return "${tenths / TENTHS}.${tenths % TENTHS}"
    }
}

/** "a pull-up bar", or "a pull-up bar and rings". */
private fun kitList(equipment: Set<Equipment>): String = equipment.sorted().map(CoachPhrases::noun).let { names ->
    if (names.size <= 1) names.joinToString() else names.dropLast(1).joinToString() + " and " + names.last()
}
