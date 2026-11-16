package app.levitx.core.skills

import app.levitx.core.load.LoadProfile

/** What a gate measures. */
public enum class GateMeasure {
    /** Clean reps in one set. */
    REPS,

    /** Seconds held in one attempt. */
    HOLD_SECONDS,
}

/**
 * One step on the way to a skill, with a clear pass mark.
 *
 * @property id Stable identifier used in storage and sync.
 * @property title Short name, for example "10 pull-ups".
 * @property measure Whether [target] counts reps or seconds.
 * @property target Reps or seconds needed to pass. At least 1.
 * @property trains One line on what the gate builds, shown when the person taps it.
 * @property loadProfile How much of the body this exercise moves, for working out the load of
 *   each rep. Null for holds and moves without a measured or reasoned share.
 * @throws IllegalArgumentException if [target] is below 1.
 */
public data class Gate(
    val id: String,
    val title: String,
    val measure: GateMeasure,
    val target: Int,
    val trains: String,
    val loadProfile: LoadProfile? = null,
) {
    init {
        require(target >= 1) { "target must be at least 1" }
    }
}

/**
 * Where someone is on a [SkillPath].
 *
 * @property currentIndex Index of the first gate not yet passed, or null once the path is done.
 * @property gatesRemaining Gates left to pass, including the current one.
 * @property currentFraction Progress toward the current gate, from 0 to 1. 1 when complete.
 */
public data class PathProgress(val currentIndex: Int?, val gatesRemaining: Int, val currentFraction: Double) {
    /** True once every gate is passed. */
    val isComplete: Boolean
        get() = currentIndex == null
}

/** How far along someone should be before starting a program. */
public enum class SkillLevel {
    /** For someone new to training, or new to this movement. */
    BEGINNER,

    /** For someone with a base of strict pull-ups, dips, push-ups and squats. */
    INTERMEDIATE,

    /** For someone years into training, or with the intermediate skills in hand. */
    ADVANCED,
}

/**
 * An ordered list of gates leading to a skill. On the Train tab it is a program.
 *
 * Gates are passed in order: a later gate never counts while an earlier one is
 * still open, because each gate builds what the next one needs.
 *
 * @property id Stable identifier used in storage and sync.
 * @property name Skill name, for example "Muscle-up".
 * @property gates Gates from first to last. The last gate is the skill itself.
 * @property level Who it is for.
 * @property weeks A rough guide to how long it takes, training three times a
 *   week. Shown as an estimate: gates pass when they pass, not on a date.
 * @property about One or two sentences on what the skill is and what to expect.
 * @throws IllegalArgumentException if [gates] is empty or has duplicate ids, or [weeks] is below 1.
 */
public data class SkillPath(
    val id: String,
    val name: String,
    val gates: List<Gate>,
    val level: SkillLevel,
    val weeks: Int,
    val about: String,
) {
    init {
        require(gates.isNotEmpty()) { "a path needs at least one gate" }
        require(gates.map { it.id }.toSet().size == gates.size) { "gate ids must be unique" }
        require(weeks >= 1) { "weeks must be at least 1" }
    }

    /**
     * Works out progress from the person's best result per gate id.
     *
     * Missing ids count as zero, and so do negative bests.
     */
    public fun progressFor(bestByGateId: Map<String, Int>): PathProgress {
        val index = gates.indexOfFirst { gate -> (bestByGateId[gate.id] ?: 0) < gate.target }
        if (index == -1) {
            return PathProgress(currentIndex = null, gatesRemaining = 0, currentFraction = 1.0)
        }
        val gate = gates[index]
        val best = (bestByGateId[gate.id] ?: 0).coerceAtLeast(0)
        return PathProgress(
            currentIndex = index,
            gatesRemaining = gates.size - index,
            currentFraction = best.toDouble() / gate.target,
        )
    }
}
