package app.levitx.phone.workout

import app.levitx.phone.live.LiveSession

/**
 * What the cues and the workout notification need from this workout: the
 * exercise and set up next, where the person is in the whole workout, and the
 * rest and timer clocks.
 */
internal fun WorkoutUiState.Active.toLiveSession(): LiveSession {
    val plans = run.session.plans
    val index = run.exerciseIndex
    return LiveSession(
        exerciseName = plan.exercise.name,
        exerciseNumber = index + 1,
        exerciseCount = plans.size,
        setNumber = run.nextSetIndex + 1,
        setCount = plan.sets.size,
        setsPlanned = plans.map { it.sets.size },
        // Exercises before this one are behind the person, done or skipped.
        setsBehind = plans.take(index).sumOf { it.sets.size } + run.logged[index].size,
        restEndsAtElapsedMillis = run.restEndsAtElapsedMillis,
        timer = timer,
        target = nextSet.target,
        passedGate = passed?.gate?.id,
        sounds = soundsOn,
        buzzes = buzzesOn,
    )
}
