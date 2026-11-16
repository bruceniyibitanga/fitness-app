package app.levitx.phone.workout

import app.levitx.core.session.SetLoad
import app.levitx.core.skills.Gate
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath

/*
 * Passing a skill gate during a workout. Any strict set that reaches the
 * gate's target passes it, whether it was a planned attempt or not: asking
 * someone to repeat a set they have just done would be friction for nothing.
 * Because the next gate is harder, the app checks the set met the standard
 * before it counts, and then says so properly.
 */

/**
 * A set waiting for the person to confirm it met the gate's standard.
 *
 * @property value Reps or seconds.
 * @property repsInReserve Reps left in the tank, or null for holds.
 * @property load The vest, belt or band used.
 */
data class PendingSet(val value: Int, val repsInReserve: Int?, val load: SetLoad)

/**
 * A gate just passed, for the screen that marks it.
 *
 * @property gate The gate.
 * @property path The path it is on.
 * @property value What the set reached.
 * @property was The best before this set.
 */
data class GatePass(val gate: Gate, val path: SkillPath, val value: Int, val was: Int) {
    /** Which gate this is on the path, from 1. */
    val number: Int
        get() = path.gates.indexOf(gate) + 1

    /** The gate that opens now, or null when this was the last. */
    val next: Gate?
        get() = path.gates.getOrNull(number)
}

/** What can be done about a gate during a workout. */
enum class GateAction {
    /** Put today's attempt off: do it last, as an ordinary best effort. */
    DEFER,

    /** Every rep met the standard: log the set and pass the gate. */
    CONFIRM,

    /** Not every rep did: go back and correct the count. */
    RECOUNT,

    /** Close the gate-passed screen and carry on. */
    CARRY_ON,
}

/**
 * True when a set of [value] with [load] would pass the gate the exercise up
 * next trains: hard work, no band help, at or past the target, on a gate not
 * yet passed. A vest is fine: it only makes the set harder.
 */
internal fun passesGate(state: WorkoutUiState.Active, value: Int, load: SetLoad): Boolean {
    val gate = state.gate ?: return false
    return state.plan.isHard &&
        load.assistanceKg == 0.0 &&
        value >= gate.target &&
        (state.gateBests[gate.id] ?: 0) < gate.target
}

/**
 * [state] after a set of [value] with [load] has been accepted: the pending
 * set cleared and, when it passed the gate, the pass to show and the new best
 * remembered so the same gate isn't checked twice.
 */
internal fun afterSet(state: WorkoutUiState.Active, value: Int, load: SetLoad): WorkoutUiState.Active {
    val gate = state.gate
    val path = gate?.let { SkillCatalogue.all.firstOrNull { path -> gate in path.gates } }
    val pass = if (gate != null && path != null && passesGate(state, value, load)) {
        GatePass(gate, path, value, was = state.gateBests[gate.id] ?: 0)
    } else {
        null
    }
    return state.copy(
        pending = null,
        passed = pass,
        gateBests = if (pass == null) state.gateBests else state.gateBests + (pass.gate.id to value),
    )
}
