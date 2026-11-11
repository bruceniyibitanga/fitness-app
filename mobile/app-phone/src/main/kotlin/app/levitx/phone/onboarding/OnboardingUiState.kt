package app.levitx.phone.onboarding

import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.SkillTraining
import app.levitx.core.load.LoadLimits
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
 * @property days Training days.
 * @property minutes Time per session.
 * @property equipment Kit they have. Empty until they pick some: nothing is assumed.
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
    val days: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
    val minutes: Int = CoachProfile.DEFAULT_SESSION_MINUTES,
    val equipment: Set<Equipment> = emptySet(),
    val firstSession: PlannedSession? = null,
    val saving: Boolean = false,
    val unavailable: Boolean = false,
) {
    /** The bodyweight typed, or null when the field is empty or invalid. */
    val bodyweightKg: Double?
        get() = parseKg(bodyweightText)?.takeIf { it in LoadLimits.MIN_BODYWEIGHT_KG..LoadLimits.MAX_BODYWEIGHT_KG }

    /** True when something is typed in the bodyweight field that isn't a usable weight. */
    val bodyweightInvalid: Boolean
        get() = bodyweightText.isNotBlank() && bodyweightKg == null

    /** True when the step on screen has what it needs to move on. */
    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.WELCOME, OnboardingStep.READY -> true
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
