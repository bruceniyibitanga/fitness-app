package app.levitx.core.history

import app.levitx.core.coach.AthleteModel

/**
 * Takes a deleted workout back out of what the coach knows.
 *
 * What it can take back exactly: the workout's place in the recent history
 * (so the week's sets drop), each exercise's mark for it and the count of
 * times trained, and any best it set, which goes back to the best of the
 * workouts that remain.
 *
 * What it leaves: the targets and rungs the coach moved after it. Each move
 * also rests on the workouts since, so undoing one would be a guess. The
 * coach corrects them over the next sessions, as it would after any
 * unusual workout.
 */
public object Forgetting {
    /**
     * [model] without [workout], given the [remaining] workouts.
     *
     * @param remaining Every stored workout except [workout].
     */
    public fun forget(model: AthleteModel, workout: WorkoutRecord, remaining: List<WorkoutRecord>): AthleteModel {
        val at = workout.finishedAtEpochMillis
        val ids = workout.exercises.map { it.exerciseId }.toSet()
        val exercises = model.exercises.mapValues { (id, state) ->
            val mark = state.marks.firstOrNull { it.atEpochMillis == at }
            if (id !in ids || mark == null) {
                state
            } else {
                state.copy(
                    best = bestOf(remaining, id),
                    timesTrained = (state.timesTrained - 1).coerceAtLeast(0),
                    marks = state.marks - mark,
                )
            }
        }
        return model.copy(
            history = model.history.filterNot { it.finishedAtEpochMillis == at },
            exercises = exercises,
            gateBests = gateBests(model.gateBests, ids, remaining),
        )
    }

    /**
     * Gate bests after the workout goes: unchanged for gates it didn't train,
     * otherwise the best strict set in [remaining], or gone when there is none.
     */
    public fun gateBests(
        before: Map<String, Int>,
        trained: Set<String>,
        remaining: List<WorkoutRecord>,
    ): Map<String, Int> =
        before.mapValues { (gateId, best) -> if (gateId in trained) bestOf(remaining, gateId) else best }
            .filterValues { it > 0 }

    /** The most reps or seconds in one strict set of [exerciseId] in [workouts], or 0. */
    private fun bestOf(workouts: List<WorkoutRecord>, exerciseId: String): Int = workouts
        .flatMap { workout -> workout.exercises.filter { it.exerciseId == exerciseId } }
        .flatMap { it.sets }
        .filter { it.countsTowardGate }
        .maxOfOrNull { it.value } ?: 0
}
