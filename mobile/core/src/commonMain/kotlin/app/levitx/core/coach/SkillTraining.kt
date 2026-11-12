package app.levitx.core.coach

import app.levitx.core.skills.Gate
import app.levitx.core.skills.SkillPath

/**
 * How each skill gate is trained: the movement it belongs to and the kit it
 * needs. This is what lets gate work count toward weekly volume and recovery
 * like any other exercise.
 *
 * A gate missing from here can't be planned, so a test checks every gate in
 * the skill catalogue has an entry.
 */
public object SkillTraining {
    private data class Setup(
        val name: String,
        val pattern: MovementPattern,
        val equipment: Set<Equipment>,
        val lowerSeconds: Int? = null,
    )

    private val bar = setOf(Equipment.PULL_UP_BAR)

    private val setups: Map<String, Setup> = mapOf(
        "first_pull_up.dead_hang" to Setup("Dead hang", MovementPattern.VERTICAL_PULL, bar),
        "first_pull_up.scapular_pulls" to Setup("Scapular pull-up", MovementPattern.VERTICAL_PULL, bar),
        "first_pull_up.flexed_arm_hang" to Setup("Chin-over-bar hold", MovementPattern.VERTICAL_PULL, bar),
        "first_pull_up.negatives" to Setup(
            "Slow negative pull-up",
            MovementPattern.VERTICAL_PULL,
            bar,
            lowerSeconds = CoachTuning.NEGATIVE_LOWER_SECONDS,
        ),
        "first_pull_up.pull_up" to Setup("Pull-up", MovementPattern.VERTICAL_PULL, bar),
        "muscle_up.dead_hang" to Setup("Dead hang", MovementPattern.VERTICAL_PULL, bar),
        "muscle_up.dips" to Setup("Straight-bar dip", MovementPattern.VERTICAL_PUSH, bar),
        "muscle_up.pull_ups" to Setup("Pull-up", MovementPattern.VERTICAL_PULL, bar),
        "muscle_up.chest_to_bar" to Setup("Chest-to-bar pull-up", MovementPattern.VERTICAL_PULL, bar),
        "muscle_up.false_grip_hang" to Setup("False-grip hang", MovementPattern.VERTICAL_PULL, bar),
        "muscle_up.muscle_up" to Setup("Muscle-up", MovementPattern.VERTICAL_PULL, bar),
    )

    /**
     * The exercise that trains [gate], or null for a gate this table doesn't know.
     * It shares the gate's id, so its history is stored under the gate.
     */
    public fun exerciseFor(gate: Gate): Exercise? = setups[gate.id]?.let { setup ->
        Exercise(
            id = gate.id,
            name = setup.name,
            pattern = setup.pattern,
            measure = gate.measure,
            range = 1..gate.target,
            equipment = setup.equipment,
            loadProfile = gate.loadProfile,
            lowerSeconds = setup.lowerSeconds,
        )
    }

    /** The movement patterns the gates of [path] train. These get the most weekly volume. */
    public fun patternsOf(path: SkillPath): Set<MovementPattern> =
        path.gates.mapNotNull { setups[it.id]?.pattern }.toSet()
}
