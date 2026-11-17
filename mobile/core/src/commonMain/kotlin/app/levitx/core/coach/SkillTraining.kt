package app.levitx.core.coach

import app.levitx.core.skills.Gate
import app.levitx.core.skills.SkillPath

/**
 * How each skill gate is trained: the movement it belongs to and the kit it
 * needs. This is what lets gate work count toward weekly volume and recovery
 * like any other exercise.
 *
 * A gate missing from here can't be planned, so a test checks every gate in
 * the skill catalogue has an entry. The entries are in [gateSetups].
 */
public object SkillTraining {
    /**
     * The exercise that trains [gate], or null for a gate this table doesn't know.
     * It shares the gate's id, so its history is stored under the gate.
     */
    public fun exerciseFor(gate: Gate): Exercise? = gateSetups[gate.id]?.let { setup ->
        Exercise(
            id = gate.id,
            name = setup.name,
            pattern = setup.pattern,
            measure = gate.measure,
            range = 1..gate.target,
            equipment = setup.equipment,
            loadProfile = gate.loadProfile,
            lowerSeconds = if (setup.lowering) CoachTuning.NEGATIVE_LOWER_SECONDS else null,
            isPerSide = setup.perSide,
        )
    }

    /** The movement patterns the gates of [path] train. These get the most weekly volume. */
    public fun patternsOf(path: SkillPath): Set<MovementPattern> =
        path.gates.mapNotNull { gateSetups[it.id]?.pattern }.toSet()

    /** Every piece of kit some gate of [path] needs. */
    public fun kitFor(path: SkillPath): Set<Equipment> =
        path.gates.flatMap { gateSetups[it.id]?.equipment.orEmpty() }.toSet()
}
