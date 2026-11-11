package app.levitx.core.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoachPhrasesTest {
    private val reasons: List<CoachReason> = listOf(
        CoachReason.StillLearning,
        CoachReason.TargetRaised("Push-up"),
        CoachReason.MovedUp("Push-up"),
        CoachReason.EasedBack("Push-up"),
        CoachReason.NewThisBlock("Reverse lunge"),
        CoachReason.GoalFirst("First pull-up"),
        CoachReason.ClosingGap(MovementPattern.SQUAT, 4),
        CoachReason.FreshestFirst(MovementPattern.HINGE),
        CoachReason.FitsYourTime(40),
        CoachReason.YourKindOfSession,
        CoachReason.Variety,
        CoachReason.Recovering(MovementPattern.VERTICAL_PULL),
        CoachReason.VolumeCapped(MovementPattern.VERTICAL_PULL),
        CoachReason.WorkingAround(BodyArea.ELBOW),
        CoachReason.HoldingSteady,
        CoachReason.RestDay,
    )

    private val signals: List<ReadinessSignal> = listOf(
        ReadinessSignal.PerformanceDrop("Pull-up", listOf(8, 6, 5)),
        ReadinessSignal.FeltHard("Pull-up"),
        ReadinessSignal.FastWeightLoss(-0.0149),
        ReadinessSignal.LongGap(21),
        ReadinessSignal.LighterWeek,
    )

    @Test
    fun everyReasonAndSignalHasItsOwnSentence() {
        val sentences = reasons.map(CoachPhrases::describe) + signals.map(CoachPhrases::describe)
        assertEquals(sentences.size, sentences.toSet().size)
        assertTrue(sentences.all { it.isNotBlank() && it.endsWith(".") })
    }

    @Test
    fun sentencesAreFilledFromTheData() {
        assertEquals(
            "More squatting today: you're 4 sets short this week.",
            CoachPhrases.describe(CoachReason.ClosingGap(MovementPattern.SQUAT, 4)),
        )
        assertEquals(
            "More squatting today: you're 1 set short this week.",
            CoachPhrases.describe(CoachReason.ClosingGap(MovementPattern.SQUAT, 1)),
        )
        assertEquals("Pull-up: 8 → 6 → 5 over your last sessions.", CoachPhrases.describe(signals.first()))
        assertEquals("Your weight is down about 1.5% a week.", CoachPhrases.describe(signals[2]))
    }

    @Test
    fun weightIsNeverPraisedOrGivenATarget() {
        val aboutWeight = listOf(CoachPhrases.describe(CoachReason.HoldingSteady), CoachPhrases.describe(signals[2]))
        val judging = listOf("great", "well done", "good", "goal", "target weight", "keep it up")
        assertTrue(aboutWeight.none { sentence -> judging.any { it in sentence.lowercase() } })
    }

    @Test
    fun everyPatternAndAreaHasAName() {
        assertEquals(MovementPattern.entries.size, MovementPattern.entries.map(CoachPhrases::noun).toSet().size)
        assertEquals(BodyArea.entries.size, BodyArea.entries.map(CoachPhrases::noun).toSet().size)
    }
}
