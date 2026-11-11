package app.levitx.testing

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.ExerciseLookup
import app.levitx.core.coach.ExercisePlan
import app.levitx.core.coach.Intensity
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.SkillTraining
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.session.PlannedSet
import app.levitx.core.skills.Gate
import app.levitx.data.coach.CoachState
import java.time.DayOfWeek

/** Someone working toward a first pull-up with a bar at home, who can't do one yet. */
fun newcomer(): AthleteModel = AthleteModel.starting(
    CoachProfile(goalPathId = "first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR)),
    PullUpAnswer.NOT_YET,
)

/** [newcomer] training three days a week. */
fun coachState(model: AthleteModel = newcomer()): CoachState =
    CoachState(model, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

/** The workout the coach would plan for [model] at [nowEpochMillis], just started. */
fun startedWorkout(model: AthleteModel = newcomer(), nowEpochMillis: Long, id: String = "workout-1"): WorkoutRun =
    WorkoutRun(
        id = id,
        session = checkNotNull(Coach().recommend(model, nowEpochMillis).session) { "the coach planned a rest day" },
        startedAtEpochMillis = nowEpochMillis,
    )

/**
 * A short session with [gate]'s skill work first (two work sets of
 * [target], then a best effort) and two sets of bodyweight squats after it.
 */
fun skillSession(gate: Gate, target: Int = 4): PlannedSession = PlannedSession(
    listOf(
        ExercisePlan(
            exercise = checkNotNull(SkillTraining.exerciseFor(gate)) { "${gate.id} has no exercise" },
            sets = listOf(PlannedSet(target, false), PlannedSet(target, false), PlannedSet(target, true)),
            restSeconds = SKILL_REST_SECONDS,
            repsInReserve = 2,
            intensity = Intensity.HARD,
            gateId = gate.id,
        ),
        ExercisePlan(
            exercise = checkNotNull(ExerciseLookup.byId("squat.bodyweight")),
            sets = List(2) { PlannedSet(SQUAT_REPS, false) },
            restSeconds = SQUAT_REST_SECONDS,
            repsInReserve = 2,
            intensity = Intensity.HARD,
        ),
    ),
)

private const val SKILL_REST_SECONDS = 150
private const val SQUAT_REST_SECONDS = 90
private const val SQUAT_REPS = 12
