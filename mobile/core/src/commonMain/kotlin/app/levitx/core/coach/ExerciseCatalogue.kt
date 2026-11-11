package app.levitx.core.coach

import app.levitx.core.coach.Equipment.BAND
import app.levitx.core.coach.Equipment.BOX
import app.levitx.core.coach.Equipment.DIP_BARS
import app.levitx.core.coach.Equipment.LOW_BAR
import app.levitx.core.coach.Equipment.PULL_UP_BAR
import app.levitx.core.coach.Equipment.RINGS
import app.levitx.core.coach.MovementPattern.CORE
import app.levitx.core.coach.MovementPattern.HINGE
import app.levitx.core.coach.MovementPattern.HORIZONTAL_PULL
import app.levitx.core.coach.MovementPattern.HORIZONTAL_PUSH
import app.levitx.core.coach.MovementPattern.SQUAT
import app.levitx.core.coach.MovementPattern.VERTICAL_PULL
import app.levitx.core.coach.MovementPattern.VERTICAL_PUSH
import app.levitx.core.load.LoadCatalogue
import app.levitx.core.load.LoadProfile
import app.levitx.core.skills.GateMeasure

/**
 * The exercises the coach plans with, as one ladder per movement pattern.
 *
 * The ladders follow published calisthenics progressions, mainly the
 * r/bodyweightfitness Recommended Routine. docs/app/COACH-ENGINE.md lists the
 * source each exercise and rung was checked against. Like the skill gates,
 * they still need review by a qualified coach before release. Only exercises
 * with a measured or reasoned bodyweight share carry a load profile; the rest
 * show no kilograms rather than a made-up figure.
 */
public object ExerciseCatalogue {
    private val bar = setOf(PULL_UP_BAR)

    private val verticalPull = Ladder(
        VERTICAL_PULL,
        listOf(
            Rung(listOf(hold("vertical_pull.dead_hang", "Dead hang", VERTICAL_PULL, 10..30, bar))),
            Rung(listOf(reps("vertical_pull.scapular_pull_up", "Scapular pull-up", VERTICAL_PULL, 5..10, bar))),
            Rung(
                listOf(
                    pull("vertical_pull.negative", "Slow negative pull-up", 3..5),
                    pull("vertical_pull.band_pull_up", "Band-assisted pull-up", 5..8).needing(BAND),
                ),
            ),
            Rung(
                listOf(
                    pull("vertical_pull.pull_up", "Pull-up", 3..8).moving(LoadCatalogue.pullUp),
                    pull("vertical_pull.chin_up", "Chin-up", 3..8),
                ),
            ),
            Rung(
                listOf(
                    pull("vertical_pull.chest_to_bar", "Chest-to-bar pull-up", 3..8),
                    pull("vertical_pull.wide_pull_up", "Wide-grip pull-up", 3..8),
                ),
            ),
        ),
    )

    private val horizontalPull = Ladder(
        HORIZONTAL_PULL,
        listOf(
            Rung(
                listOf(
                    row("horizontal_pull.row_knees_bent", "Australian row, knees bent", 6..12, LOW_BAR),
                    row("horizontal_pull.ring_row_upright", "Ring row, standing tall", 6..12, RINGS),
                    row("horizontal_pull.band_row", "Seated band row", 10..15, BAND),
                ),
            ),
            Rung(
                listOf(
                    row("horizontal_pull.row", "Australian row", 6..12, LOW_BAR),
                    row("horizontal_pull.ring_row", "Ring row", 6..12, RINGS),
                ),
            ),
            Rung(
                listOf(
                    row("horizontal_pull.row_feet_raised", "Australian row, feet raised", 6..12, LOW_BAR),
                    row("horizontal_pull.ring_row_feet_raised", "Ring row, feet raised", 6..12, RINGS),
                ),
            ),
        ),
    )

    private val verticalPush = Ladder(
        VERTICAL_PUSH,
        listOf(
            Rung(listOf(reps("vertical_push.pike_hands_raised", "Pike push-up, hands raised", VERTICAL_PUSH, 5..10))),
            Rung(listOf(reps("vertical_push.pike", "Pike push-up", VERTICAL_PUSH, 5..10))),
            // Slow negatives come before full dips, as in the r/bodyweightfitness
            // dip progression, so nobody meets a full dip before they can lower through one.
            Rung(
                listOf(
                    reps("vertical_push.pike_feet_raised", "Pike push-up, feet raised", VERTICAL_PUSH, 5..10),
                    reps("vertical_push.negative_dip", "Slow negative dip", VERTICAL_PUSH, 3..5, setOf(DIP_BARS))
                        .alsoTraining(HORIZONTAL_PUSH),
                ),
            ),
            Rung(
                listOf(
                    reps("vertical_push.wall_negative", "Wall handstand push-up negative", VERTICAL_PUSH, 3..5),
                    reps("vertical_push.dip", "Dip", VERTICAL_PUSH, 5..10, setOf(DIP_BARS))
                        .alsoTraining(HORIZONTAL_PUSH)
                        .moving(LoadCatalogue.dip),
                ),
            ),
        ),
    )

    private val horizontalPush = Ladder(
        HORIZONTAL_PUSH,
        listOf(
            // Nothing is assumed: the first rung needs only a wall. The box rungs
            // fill in between for people who have one and are skipped by those who don't.
            Rung(listOf(reps("horizontal_push.wall", "Wall push-up", HORIZONTAL_PUSH, PUSH_UP_RANGE))),
            Rung(listOf(pushUp("horizontal_push.hands_61cm", LoadCatalogue.pushUpHandsOn61cmBox).needing(BOX))),
            Rung(listOf(pushUp("horizontal_push.knees", LoadCatalogue.pushUpKnees))),
            Rung(listOf(pushUp("horizontal_push.hands_30cm", LoadCatalogue.pushUpHandsOn30cmBox).needing(BOX))),
            Rung(
                listOf(
                    pushUp("horizontal_push.push_up", LoadCatalogue.pushUp),
                    reps("horizontal_push.close_grip", "Close-grip push-up", HORIZONTAL_PUSH, PUSH_UP_RANGE)
                        .alsoTraining(VERTICAL_PUSH),
                ),
            ),
            Rung(listOf(pushUp("horizontal_push.feet_30cm", LoadCatalogue.pushUpFeetOn30cmBox).needing(BOX))),
            Rung(listOf(pushUp("horizontal_push.feet_61cm", LoadCatalogue.pushUpFeetOn61cmBox).needing(BOX))),
        ),
    )

    private val squat = Ladder(
        SQUAT,
        listOf(
            Rung(listOf(reps("squat.box", "Squat to a chair", SQUAT, 8..15))),
            Rung(listOf(reps("squat.bodyweight", "Bodyweight squat", SQUAT, 10..20))),
            Rung(
                listOf(
                    reps("squat.split", "Split squat", SQUAT, 8..12).perSide(),
                    reps("squat.reverse_lunge", "Reverse lunge", SQUAT, 8..12).perSide(),
                ),
            ),
            Rung(
                listOf(
                    reps("squat.bulgarian", "Bulgarian split squat", SQUAT, 6..12).perSide(),
                    reps("squat.step_up", "High step-up", SQUAT, 6..12, setOf(BOX)).perSide(),
                ),
            ),
            Rung(listOf(reps("squat.pistol_to_chair", "Pistol squat to a chair", SQUAT, 4..8).perSide())),
            Rung(listOf(reps("squat.pistol", "Pistol squat", SQUAT, 3..6).perSide())),
        ),
    )

    private val hinge = Ladder(
        HINGE,
        listOf(
            Rung(listOf(reps("hinge.glute_bridge", "Glute bridge", HINGE, 10..20))),
            Rung(
                listOf(
                    reps("hinge.single_leg_bridge", "Single-leg glute bridge", HINGE, 8..15).perSide(),
                    reps("hinge.hip_thrust", "Hip thrust, shoulders raised", HINGE, 10..20),
                ),
            ),
            Rung(listOf(reps("hinge.single_leg_deadlift", "Single-leg deadlift", HINGE, 8..12).perSide())),
            Rung(listOf(reps("hinge.sliding_curl", "Sliding leg curl", HINGE, 6..12))),
            Rung(listOf(reps("hinge.nordic_negative", "Nordic curl negative", HINGE, 3..6))),
        ),
    )

    private val core = Ladder(
        CORE,
        listOf(
            Rung(
                listOf(
                    hold("core.plank", "Plank", CORE, 20..45),
                    reps("core.dead_bug", "Dead bug", CORE, 8..12).perSide(),
                ),
            ),
            Rung(
                listOf(
                    hold("core.hollow_hold", "Hollow body hold", CORE, 15..40),
                    reps("core.lying_leg_raise", "Lying leg raise", CORE, 8..15),
                ),
            ),
            Rung(
                listOf(
                    reps("core.hanging_knee_raise", "Hanging knee raise", CORE, 6..12, bar),
                    hold("core.tuck_l_sit", "Tuck L-sit hold", CORE, 10..20),
                ),
            ),
            Rung(listOf(reps("core.hanging_leg_raise", "Hanging leg raise", CORE, 5..10, bar))),
        ),
    )

    /** Every ladder, one per movement pattern. */
    public val ladders: List<Ladder> =
        listOf(verticalPull, horizontalPull, verticalPush, horizontalPush, squat, hinge, core)

    private val laddersByPattern: Map<MovementPattern, Ladder> = ladders.associateBy { it.pattern }

    /** Every exercise on every ladder. */
    public val all: List<Exercise> = ladders.flatMap { ladder -> ladder.rungs.flatMap { it.options } }

    private val exercisesById: Map<String, Exercise> = all.associateBy { it.id }

    /** The ladder for [pattern]. Every pattern has one. */
    public fun ladder(pattern: MovementPattern): Ladder = laddersByPattern.getValue(pattern)

    /** Finds an exercise by its [Exercise.id], or returns null. */
    public fun byId(id: String): Exercise? = exercisesById[id]

    /**
     * Kit that would open up a pattern the person can't train at all with what
     * they [owned]. The apps ask about these when it matters ("Got rings?"),
     * instead of listing every piece of kit at setup.
     */
    public fun equipmentWorthAskingAbout(owned: Set<Equipment>): Set<Equipment> = ladders
        .filter { it.usableRungAtOrBelow(it.rungs.lastIndex, owned) == null }
        .flatMap { ladder -> ladder.rungs.first().options.flatMap { it.equipment - owned } }
        .toSet()
}

/** Every push-up variation is trained in the same rep range, so moving up a rung changes only the load. */
private val PUSH_UP_RANGE: IntRange = 6..12

private fun reps(
    id: String,
    name: String,
    pattern: MovementPattern,
    range: IntRange,
    equipment: Set<Equipment> = emptySet(),
): Exercise = Exercise(id, name, pattern, GateMeasure.REPS, range, equipment)

private fun hold(
    id: String,
    name: String,
    pattern: MovementPattern,
    range: IntRange,
    equipment: Set<Equipment> = emptySet(),
): Exercise = Exercise(id, name, pattern, GateMeasure.HOLD_SECONDS, range, equipment)

/** A bent-arm pull on the bar, which also works the rowing muscles. */
private fun pull(id: String, name: String, range: IntRange): Exercise =
    reps(id, name, VERTICAL_PULL, range, setOf(PULL_UP_BAR)).alsoTraining(HORIZONTAL_PULL)

private fun row(id: String, name: String, range: IntRange, kit: Equipment): Exercise =
    reps(id, name, HORIZONTAL_PULL, range, setOf(kit)).alsoTraining(VERTICAL_PULL)

private fun pushUp(id: String, profile: LoadProfile): Exercise =
    reps(id, profile.name, HORIZONTAL_PUSH, PUSH_UP_RANGE).alsoTraining(VERTICAL_PUSH).moving(profile)

private fun Exercise.alsoTraining(pattern: MovementPattern): Exercise = copy(alsoTrains = setOf(pattern))

private fun Exercise.moving(profile: LoadProfile): Exercise = copy(loadProfile = profile)

private fun Exercise.needing(kit: Equipment): Exercise = copy(equipment = equipment + kit)

private fun Exercise.perSide(): Exercise = copy(isPerSide = true)
