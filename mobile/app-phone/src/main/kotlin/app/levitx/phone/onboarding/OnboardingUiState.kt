package app.levitx.phone.onboarding

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.Exercise
import app.levitx.core.coach.ExerciseCatalogue
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.QuickTest
import app.levitx.core.coach.SkillTraining
import app.levitx.core.load.LoadLimits
import app.levitx.core.load.WeightUnit
import app.levitx.core.skills.SkillCatalogue
import app.levitx.phone.ui.parseKg
import java.time.DayOfWeek

/** The steps of setup, in order. */
enum class OnboardingStep {
    /** What Levitx is. */
    WELCOME,

    /** What they are training for. */
    GOAL,

    /** All-round or focused, whether they can do a pull-up, and bodyweight. */
    HOW,

    /** Training days, time per session and kit. */
    WEEK,

    /** The optional quick test, one movement at a time. */
    TEST,

    /** The plan, with the first session. */
    READY,
}

/** Session lengths offered at setup, in minutes. */
val SessionLengths: List<Int> = listOf(20, 30, 45, 60)

/**
 * Everything setup has been told so far. Nothing is saved until the last step.
 *
 * @property step The step on screen.
 * @property goalChosen True once a goal is picked. [goalPathId] null then means general fitness.
 * @property goalPathId The skill path picked, or null for general fitness.
 * @property style All-round or focused. Only asked with a skill goal.
 * @property pullUp The answer to "Can you do a strict pull-up?", or null before it is given.
 * @property bodyweightText Bodyweight as typed. Optional.
 * @property bodyweightUnit The unit it was typed in.
 * @property days Training days.
 * @property minutes Time per session.
 * @property equipment Kit they have. Empty until they pick some: nothing is assumed.
 * @property testIndex Which of [tests] is on screen during the quick test.
 * @property testCounts Reps counted for each test so far. A test not in here counts as none.
 * @property testsDone How many of [tests] were confirmed with "next". Only these
 *   reach the coach, so skipping keeps what was done and drops the rest.
 * @property firstSession The coach's first session, worked out for the last step.
 * @property saving True while the answers are being saved.
 * @property unavailable True when the data couldn't be saved on this device.
 */
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val goalChosen: Boolean = false,
    val goalPathId: String? = null,
    val style: PlanStyle = PlanStyle.ALL_ROUND,
    val pullUp: PullUpAnswer? = null,
    val bodyweightText: String = "",
    val bodyweightUnit: WeightUnit = WeightUnit.KG,
    val days: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
    val minutes: Int = CoachProfile.DEFAULT_SESSION_MINUTES,
    val equipment: Set<Equipment> = emptySet(),
    val testIndex: Int = 0,
    val testCounts: Map<QuickTest, Int> = emptyMap(),
    val testsDone: Int = 0,
    val firstSession: PlannedSession? = null,
    val saving: Boolean = false,
    val unavailable: Boolean = false,
) {
    /** The bodyweight typed, in kilograms, or null when the field is empty or invalid. */
    val bodyweightKg: Double?
        get() = parseKg(bodyweightText)?.let(bodyweightUnit::toKg)
            ?.takeIf { it in LoadLimits.MIN_BODYWEIGHT_KG..LoadLimits.MAX_BODYWEIGHT_KG }

    /** True when something is typed in the bodyweight field that isn't a usable weight. */
    val bodyweightInvalid: Boolean
        get() = bodyweightText.isNotBlank() && bodyweightKg == null

    /** True when the step on screen has what it needs to move on. */
    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.WELCOME, OnboardingStep.TEST, OnboardingStep.READY -> true
            OnboardingStep.GOAL -> goalChosen
            OnboardingStep.HOW -> pullUp != null && !bodyweightInvalid
            OnboardingStep.WEEK -> days.isNotEmpty()
        }

    /**
     * Kit the chosen skill's gates need that isn't picked yet. Setup says so,
     * so nobody finds out from a plan that quietly leaves their goal out.
     */
    val missingGoalKit: Set<Equipment>
        get() = goalPathId?.let(SkillCatalogue::byId)?.gates
            ?.flatMap { gate -> SkillTraining.exerciseFor(gate)?.equipment.orEmpty() }
            ?.toSet()
            ?.minus(equipment)
            .orEmpty()

    /** The quick tests the picked kit allows, in order. */
    val tests: List<QuickTest>
        get() = QuickTest.forKit(equipment)

    /** The test on screen, or null outside the quick test. */
    val test: QuickTest?
        get() = tests.getOrNull(testIndex).takeIf { step == OnboardingStep.TEST }

    /** Reps counted for the test on screen. */
    val testCount: Int
        get() = test?.let { testCounts[it] } ?: 0

    /** The confirmed quick test results, for the coach. Empty when the test was skipped. */
    val tested: Map<QuickTest, Int>
        get() = tests.take(testsDone).associateWith { testCounts[it] ?: 0 }

    /** The model setup would start the coach with. */
    val startingModel: AthleteModel
        get() = AthleteModel.starting(profile, pullUp ?: PullUpAnswer.NOT_SURE, tested)

    /**
     * Where [count] on the test on screen would start that movement: the
     * exercise, and its work set target when the test exercise itself is where
     * it starts. Null outside the test or with nothing counted.
     */
    val testStart: Pair<Exercise, Int?>?
        get() {
            val test = test?.takeIf { testCount > 0 } ?: return null
            val model = AthleteModel.starting(profile, PullUpAnswer.NOT_SURE, mapOf(test to testCount))
            val ladder = ExerciseCatalogue.ladder(test.exercise.pattern)
            val exercise = ladder.exerciseAt(model.rung(ladder.pattern), equipment) ?: return null
            return exercise to model.exercises[exercise.id]?.target
        }

    /** The profile these answers describe. */
    val profile: CoachProfile
        get() = CoachProfile(
            goalPathId = goalPathId,
            style = if (goalPathId == null) PlanStyle.ALL_ROUND else style,
            sessionsPerWeek = days.size.coerceIn(
                CoachProfile.MIN_SESSIONS_PER_WEEK,
                CoachProfile.MAX_SESSIONS_PER_WEEK,
            ),
            sessionMinutes = minutes,
            equipment = equipment,
        )
}
