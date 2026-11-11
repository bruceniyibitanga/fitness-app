package app.levitx.phone

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WorkoutRun
import app.levitx.data.coach.CoachState
import java.time.DayOfWeek

/** Someone working toward a first pull-up with a bar at home, who can't do one yet. */
internal fun newcomer(): AthleteModel = AthleteModel.starting(
    CoachProfile(goalPathId = "first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR)),
    PullUpAnswer.NOT_YET,
)

/** [newcomer] training three days a week. */
internal fun coachState(model: AthleteModel = newcomer()): CoachState =
    CoachState(model, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

/** The workout the coach would plan for [model] at [nowEpochMillis], just started. */
internal fun startedWorkout(
    model: AthleteModel = newcomer(),
    nowEpochMillis: Long,
    id: String = "workout-1",
): WorkoutRun = WorkoutRun(
    id = id,
    session = checkNotNull(Coach().recommend(model, nowEpochMillis).session) { "the coach planned a rest day" },
    startedAtEpochMillis = nowEpochMillis,
)
