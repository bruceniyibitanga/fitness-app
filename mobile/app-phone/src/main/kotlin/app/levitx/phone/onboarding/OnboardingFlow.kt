package app.levitx.phone.onboarding

import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.load.WeightUnit
import java.time.DayOfWeek

/*
 * How setup moves between steps and through the quick test. Pure functions of
 * the answers so far, so the view model only applies them.
 */

/**
 * The next step, if this one has what it needs. In the quick test it confirms
 * the count on screen and moves to the next movement, and after the last
 * movement on to the plan.
 */
internal fun OnboardingUiState.forward(): OnboardingUiState {
    if (!canContinue || step == OnboardingStep.READY) return this
    val testing = step == OnboardingStep.TEST
    return when {
        testing && testIndex < tests.lastIndex -> copy(testIndex = testIndex + 1, testsDone = testIndex + 1)
        testing -> copy(step = OnboardingStep.READY, testsDone = tests.size)
        else -> copy(step = OnboardingStep.entries[step.ordinal + 1], testIndex = 0)
    }
}

/** Straight to the plan from the quick test. Movements already confirmed are kept; the rest are dropped. */
internal fun OnboardingUiState.skippingTest(): OnboardingUiState =
    if (step == OnboardingStep.TEST) copy(step = OnboardingStep.READY, testsDone = testIndex) else this

/**
 * One step back, or one movement back in the quick test. From the plan it
 * returns to the movement the test was left on.
 */
internal fun OnboardingUiState.backward(): OnboardingUiState = when {
    step == OnboardingStep.WELCOME -> this

    step == OnboardingStep.TEST && testIndex > 0 -> copy(testIndex = testIndex - 1)

    step == OnboardingStep.READY -> copy(
        step = OnboardingStep.TEST,
        testIndex = testIndex.coerceAtMost(tests.lastIndex),
    )

    else -> copy(step = OnboardingStep.entries[step.ordinal - 1])
}

/** One answer given during setup. */
sealed interface SetupAnswer {
    /**
     * A skill path by id, or general fitness with null.
     *
     * @property pathId The skill path id, or null for general fitness.
     */
    data class Goal(val pathId: String?) : SetupAnswer

    /**
     * All-round or focused.
     *
     * @property style All-round or focused.
     */
    data class Style(val style: PlanStyle) : SetupAnswer

    /**
     * The answer to "Can you do a strict pull-up?".
     *
     * @property answer Yes, not yet or not sure.
     */
    data class PullUp(val answer: PullUpAnswer) : SetupAnswer

    /**
     * What is typed in the bodyweight field, and the [unit] it is in.
     *
     * @property text As typed.
     * @property unit Kilograms or pounds.
     */
    data class Bodyweight(val text: String, val unit: WeightUnit = WeightUnit.KG) : SetupAnswer

    /**
     * A training day added or removed.
     *
     * @property day The day.
     */
    data class Day(val day: DayOfWeek) : SetupAnswer

    /**
     * The time per session.
     *
     * @property minutes Minutes per session.
     */
    data class Minutes(val minutes: Int) : SetupAnswer

    /**
     * A piece of kit added or removed, or with null, no kit at all, clearing anything picked.
     *
     * @property equipment The kit, or null for none.
     */
    data class Kit(val equipment: Equipment?) : SetupAnswer

    /**
     * Reps counted for the quick test movement on screen.
     *
     * @property reps The reps counted.
     */
    data class TestCount(val reps: Int) : SetupAnswer
}

/**
 * These answers with [answer] taken in. Typed bodyweight is cut short, so
 * nothing silly can be stored, and test counts are kept between none and the
 * most the test accepts.
 */
internal fun OnboardingUiState.with(answer: SetupAnswer): OnboardingUiState = when (answer) {
    is SetupAnswer.Goal -> copy(goalChosen = true, goalPathId = answer.pathId)

    is SetupAnswer.Style -> copy(style = answer.style)

    is SetupAnswer.PullUp -> copy(pullUp = answer.answer)

    is SetupAnswer.Bodyweight -> copy(bodyweightText = answer.text.take(MAX_WEIGHT_CHARS), bodyweightUnit = answer.unit)

    is SetupAnswer.Day -> copy(days = days.toggled(answer.day))

    is SetupAnswer.Minutes -> copy(minutes = answer.minutes)

    is SetupAnswer.Kit -> copy(equipment = answer.equipment?.let(equipment::toggled) ?: emptySet())

    is SetupAnswer.TestCount -> test?.let {
        copy(testCounts = testCounts + (it to answer.reps.coerceIn(0, it.maxReps)))
    }
        ?: this
}

private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item

/** The longest bodyweight that can be typed, in characters. */
private const val MAX_WEIGHT_CHARS = 6
