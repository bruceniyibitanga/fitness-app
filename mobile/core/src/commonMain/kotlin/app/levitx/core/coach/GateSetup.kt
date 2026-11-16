package app.levitx.core.coach

import app.levitx.core.coach.Equipment.DIP_BARS
import app.levitx.core.coach.Equipment.PULL_UP_BAR
import app.levitx.core.coach.MovementPattern.CORE
import app.levitx.core.coach.MovementPattern.HINGE
import app.levitx.core.coach.MovementPattern.HORIZONTAL_PULL
import app.levitx.core.coach.MovementPattern.HORIZONTAL_PUSH
import app.levitx.core.coach.MovementPattern.SQUAT
import app.levitx.core.coach.MovementPattern.VERTICAL_PULL
import app.levitx.core.coach.MovementPattern.VERTICAL_PUSH

/**
 * How one skill gate is trained.
 *
 * @property name What the exercise is called in a session.
 * @property pattern The movement it counts toward, for weekly volume and recovery.
 *   Straight-arm holds count toward the movement they load most: levers as
 *   rows, planche work as push-ups, handstands as overhead pressing.
 * @property equipment Kit it needs. Kit the person improvises, like a chair or
 *   a wall, isn't listed; the exercise guide says what to use.
 * @property lowering True for a timed slow negative.
 * @property perSide True when the target is for each arm or leg.
 */
internal data class GateSetup(
    val name: String,
    val pattern: MovementPattern,
    val equipment: Set<Equipment> = emptySet(),
    val lowering: Boolean = false,
    val perSide: Boolean = false,
)

private val bar = setOf(PULL_UP_BAR)

private fun onBar(name: String, pattern: MovementPattern = VERTICAL_PULL) = GateSetup(name, pattern, bar)

private fun GateSetup.lowered(): GateSetup = copy(lowering = true)

private fun GateSetup.eachSide(): GateSetup = copy(perSide = true)

/** The setup for every gate on every path, by gate id. */
internal val gateSetups: Map<String, GateSetup> = mapOf(
    "first_push_up.wall" to GateSetup("Wall push-up", HORIZONTAL_PUSH),
    "first_push_up.plank" to GateSetup("Plank", CORE),
    "first_push_up.knees" to GateSetup("Knee push-up", HORIZONTAL_PUSH),
    "first_push_up.negatives" to GateSetup("Slow push-up negative", HORIZONTAL_PUSH).lowered(),
    "first_push_up.push_up" to GateSetup("Push-up", HORIZONTAL_PUSH),
    "twenty_push_ups.five" to GateSetup("Push-up", HORIZONTAL_PUSH),
    "twenty_push_ups.ten" to GateSetup("Push-up", HORIZONTAL_PUSH),
    "twenty_push_ups.fifteen" to GateSetup("Push-up", HORIZONTAL_PUSH),
    "twenty_push_ups.twenty" to GateSetup("Push-up", HORIZONTAL_PUSH),
    "first_pull_up.dead_hang" to onBar("Dead hang"),
    "first_pull_up.scapular_pulls" to onBar("Scapular pull-up"),
    "first_pull_up.flexed_arm_hang" to onBar("Chin-over-bar hold"),
    "first_pull_up.negatives" to onBar("Slow negative pull-up").lowered(),
    "first_pull_up.pull_up" to onBar("Pull-up"),
    "ten_pull_ups.three" to onBar("Pull-up"),
    "ten_pull_ups.five" to onBar("Pull-up"),
    "ten_pull_ups.chin_ups" to onBar("Chin-up"),
    "ten_pull_ups.seven" to onBar("Pull-up"),
    "ten_pull_ups.ten" to onBar("Pull-up"),
    "first_dip.support" to GateSetup("Support hold", VERTICAL_PUSH, setOf(DIP_BARS)),
    "first_dip.bench_dips" to GateSetup("Bench dip", VERTICAL_PUSH),
    "first_dip.negatives" to GateSetup("Slow negative dip", VERTICAL_PUSH, setOf(DIP_BARS)).lowered(),
    "first_dip.dip" to GateSetup("Dip", VERTICAL_PUSH, setOf(DIP_BARS)),
    "pistol_squat.deep_squat" to GateSetup("Deep squat hold", SQUAT),
    "pistol_squat.bulgarian" to GateSetup("Bulgarian split squat", SQUAT).eachSide(),
    "pistol_squat.to_chair" to GateSetup("Pistol squat to a chair", SQUAT).eachSide(),
    "pistol_squat.assisted" to GateSetup("Pistol squat, holding on", SQUAT).eachSide(),
    "pistol_squat.pistol" to GateSetup("Pistol squat", SQUAT).eachSide(),
    "shrimp_squat.lunges" to GateSetup("Reverse lunge", SQUAT).eachSide(),
    "shrimp_squat.bulgarian" to GateSetup("Bulgarian split squat", SQUAT).eachSide(),
    "shrimp_squat.hands_forward" to GateSetup("Shrimp squat, hands forward", SQUAT).eachSide(),
    "shrimp_squat.knee_down" to GateSetup("Shrimp squat, knee to the floor", SQUAT).eachSide(),
    "shrimp_squat.holding_foot" to GateSetup("Shrimp squat, holding your foot", SQUAT).eachSide(),
    "nordic_curl.single_leg_bridges" to GateSetup("Single-leg glute bridge", HINGE).eachSide(),
    "nordic_curl.sliding_curls" to GateSetup("Sliding leg curl", HINGE),
    "nordic_curl.negatives" to GateSetup("Nordic curl negative", HINGE).lowered(),
    "nordic_curl.nordic" to GateSetup("Nordic curl", HINGE),
    "nordic_curl.three" to GateSetup("Nordic curl", HINGE),
    "l_sit.hollow" to GateSetup("Hollow body hold", CORE),
    "l_sit.tuck" to GateSetup("Tuck L-sit", CORE),
    "l_sit.one_leg" to GateSetup("One-leg L-sit", CORE),
    "l_sit.l_sit" to GateSetup("L-sit", CORE),
    "l_sit.twenty" to GateSetup("L-sit", CORE),
    "dragon_flag.hollow" to GateSetup("Hollow body hold", CORE),
    "dragon_flag.leg_raises" to GateSetup("Lying leg raise", CORE),
    "dragon_flag.tuck_negatives" to GateSetup("Tuck dragon flag negative", CORE).lowered(),
    "dragon_flag.negatives" to GateSetup("Dragon flag negative", CORE).lowered(),
    "dragon_flag.dragon_flag" to GateSetup("Dragon flag", CORE),
    "handstand.wall_plank" to GateSetup("Wall plank", VERTICAL_PUSH),
    "handstand.crow" to GateSetup("Crow pose", VERTICAL_PUSH),
    "handstand.chest_to_wall" to GateSetup("Chest-to-wall handstand", VERTICAL_PUSH),
    "handstand.toes_on_wall" to GateSetup("Handstand, toes on the wall", VERTICAL_PUSH),
    "handstand.freestanding" to GateSetup("Freestanding handstand", VERTICAL_PUSH),
    "handstand.thirty" to GateSetup("Freestanding handstand", VERTICAL_PUSH),
    "handstand_push_up.pike" to GateSetup("Pike push-up, feet raised", VERTICAL_PUSH),
    "handstand_push_up.wall_hold" to GateSetup("Chest-to-wall handstand", VERTICAL_PUSH),
    "handstand_push_up.negatives" to GateSetup("Wall handstand push-up negative", VERTICAL_PUSH).lowered(),
    "handstand_push_up.one" to GateSetup("Wall handstand push-up", VERTICAL_PUSH),
    "handstand_push_up.five" to GateSetup("Wall handstand push-up", VERTICAL_PUSH),
    "muscle_up.dead_hang" to onBar("Dead hang"),
    "muscle_up.dips" to onBar("Straight-bar dip", VERTICAL_PUSH),
    "muscle_up.pull_ups" to onBar("Pull-up"),
    "muscle_up.chest_to_bar" to onBar("Chest-to-bar pull-up"),
    "muscle_up.false_grip_hang" to onBar("False-grip hang"),
    "muscle_up.muscle_up" to onBar("Muscle-up"),
    "back_lever.german_hang" to onBar("German hang", HORIZONTAL_PULL),
    "back_lever.skin_the_cat" to onBar("Skin the cat", HORIZONTAL_PULL),
    "back_lever.tuck" to onBar("Tuck back lever", HORIZONTAL_PULL),
    "back_lever.advanced_tuck" to onBar("Advanced tuck back lever", HORIZONTAL_PULL),
    "back_lever.straddle" to onBar("Straddle back lever", HORIZONTAL_PULL),
    "back_lever.full" to onBar("Back lever", HORIZONTAL_PULL),
    "front_lever.tuck" to onBar("Tuck front lever", HORIZONTAL_PULL),
    "front_lever.advanced_tuck" to onBar("Advanced tuck front lever", HORIZONTAL_PULL),
    "front_lever.one_leg" to onBar("One-leg front lever", HORIZONTAL_PULL),
    "front_lever.straddle" to onBar("Straddle front lever", HORIZONTAL_PULL),
    "front_lever.half_lay" to onBar("Half lay front lever", HORIZONTAL_PULL),
    "front_lever.full" to onBar("Front lever", HORIZONTAL_PULL),
    "tuck_planche.lean" to GateSetup("Planche lean", HORIZONTAL_PUSH),
    "tuck_planche.frog" to GateSetup("Frog stand", HORIZONTAL_PUSH),
    "tuck_planche.pseudo" to GateSetup("Pseudo-planche push-up", HORIZONTAL_PUSH),
    "tuck_planche.five" to GateSetup("Tuck planche", HORIZONTAL_PUSH),
    "tuck_planche.fifteen" to GateSetup("Tuck planche", HORIZONTAL_PUSH),
    "one_arm_push_up.twenty" to GateSetup("Push-up", HORIZONTAL_PUSH),
    "one_arm_push_up.archer" to GateSetup("Archer push-up", HORIZONTAL_PUSH).eachSide(),
    "one_arm_push_up.negatives" to GateSetup("One-arm push-up negative", HORIZONTAL_PUSH).lowered().eachSide(),
    "one_arm_push_up.one" to GateSetup("One-arm push-up", HORIZONTAL_PUSH).eachSide(),
    "one_arm_push_up.three" to GateSetup("One-arm push-up", HORIZONTAL_PUSH).eachSide(),
    "one_arm_pull_up.fifteen" to onBar("Pull-up"),
    "one_arm_pull_up.archer" to onBar("Archer pull-up").eachSide(),
    "one_arm_pull_up.one_arm_hang" to onBar("One-arm hang").eachSide(),
    "one_arm_pull_up.negatives" to onBar("One-arm pull-up negative").lowered().eachSide(),
    "one_arm_pull_up.one" to onBar("One-arm pull-up").eachSide(),
)
