package app.levitx.core.coach

/*
 * Form cues for skill gates. Most gates are the same movement as a ladder
 * exercise, so they borrow its guide through [gateGuideSources]. The rest,
 * mostly holds and straight-arm work that no ladder has, are written out in
 * [skillGuides]. ExerciseGuides.forExercise looks in both. Like every guide,
 * they still need a qualified coach's review before release.
 */

/** Gates that are a ladder exercise under another name: gate id to the ladder exercise's id. */
internal val gateGuideSources: Map<String, String> = mapOf(
    "first_push_up.wall" to "horizontal_push.wall",
    "first_push_up.plank" to "core.plank",
    "first_push_up.knees" to "horizontal_push.knees",
    "first_push_up.push_up" to "horizontal_push.push_up",
    "twenty_push_ups.five" to "horizontal_push.push_up",
    "twenty_push_ups.ten" to "horizontal_push.push_up",
    "twenty_push_ups.fifteen" to "horizontal_push.push_up",
    "twenty_push_ups.twenty" to "horizontal_push.push_up",
    "ten_pull_ups.three" to "vertical_pull.pull_up",
    "ten_pull_ups.five" to "vertical_pull.pull_up",
    "ten_pull_ups.chin_ups" to "vertical_pull.chin_up",
    "ten_pull_ups.seven" to "vertical_pull.pull_up",
    "ten_pull_ups.ten" to "vertical_pull.pull_up",
    "first_dip.negatives" to "vertical_push.negative_dip",
    "first_dip.dip" to "vertical_push.dip",
    "pistol_squat.bulgarian" to "squat.bulgarian",
    "pistol_squat.to_chair" to "squat.pistol_to_chair",
    "pistol_squat.pistol" to "squat.pistol",
    "shrimp_squat.lunges" to "squat.reverse_lunge",
    "shrimp_squat.bulgarian" to "squat.bulgarian",
    "shrimp_squat.hands_forward" to "squat.beginner_shrimp",
    "shrimp_squat.knee_down" to "squat.intermediate_shrimp",
    "shrimp_squat.holding_foot" to "squat.shrimp",
    "nordic_curl.single_leg_bridges" to "hinge.single_leg_bridge",
    "nordic_curl.sliding_curls" to "hinge.sliding_curl",
    "nordic_curl.negatives" to "hinge.nordic_negative",
    "nordic_curl.nordic" to "hinge.nordic",
    "nordic_curl.three" to "hinge.nordic",
    "l_sit.hollow" to "core.hollow_hold",
    "l_sit.tuck" to "core.tuck_l_sit",
    "l_sit.l_sit" to "core.l_sit",
    "l_sit.twenty" to "core.l_sit",
    "dragon_flag.hollow" to "core.hollow_hold",
    "dragon_flag.leg_raises" to "core.lying_leg_raise",
    "dragon_flag.tuck_negatives" to "core.tuck_dragon_flag_negative",
    "dragon_flag.negatives" to "core.dragon_flag_negative",
    "handstand_push_up.pike" to "vertical_push.pike_feet_raised",
    "handstand_push_up.negatives" to "vertical_push.wall_negative",
    "handstand_push_up.one" to "vertical_push.wall_hspu",
    "handstand_push_up.five" to "vertical_push.wall_hspu",
    "tuck_planche.pseudo" to "horizontal_push.pseudo_planche",
    "one_arm_push_up.twenty" to "horizontal_push.push_up",
    "one_arm_push_up.archer" to "horizontal_push.archer",
    "one_arm_push_up.one" to "horizontal_push.one_arm",
    "one_arm_push_up.three" to "horizontal_push.one_arm",
    "one_arm_pull_up.fifteen" to "vertical_pull.pull_up",
    "one_arm_pull_up.archer" to "vertical_pull.archer",
    "one_arm_pull_up.negatives" to "vertical_pull.one_arm_negative",
)

private val pushUpNegative = ExerciseGuide(
    cues = listOf(
        "Start at the top of a push-up, body in one straight line.",
        "Lower for 3 seconds until your chest is a fist from the floor.",
        "Drop your knees and push back up, then reset at the top.",
    ),
)

private val supportHold = ExerciseGuide(
    cues = listOf(
        "Arms locked straight, hands by your hips.",
        "Push the bars down so your shoulders sit low, away from your ears.",
        "Body still, legs together or knees bent behind you.",
    ),
)

private val benchDip = ExerciseGuide(
    cues = listOf(
        "Hands on the edge of a sturdy chair or bench behind you, fingers forwards.",
        "Bend your elbows straight back until your upper arms are level with the floor.",
        "Press back up. Bent knees make it easier, straight legs harder.",
    ),
    note = "Stop short of the bottom if the front of your shoulders aches.",
)

private val deepSquat = ExerciseGuide(
    cues = listOf(
        "Feet about shoulder width, heels flat on the floor.",
        "Sink as low as you can and stay there, chest up.",
        "Push your knees out with your elbows if they cave in.",
    ),
    note = "Heels lifting? Put a thin book under them for now and take it away as your ankles loosen.",
)

private val assistedPistol = ExerciseGuide(
    cues = listOf(
        "Stand on one leg beside a door frame or post, the other leg out in front.",
        "Sit all the way down, holding on only as much as you need.",
        "Stand back up, pulling less with your hands each week.",
    ),
)

private val oneLegLSit = ExerciseGuide(
    cues = listOf(
        "From a tuck L-sit, straighten one leg out in front, level with your hips.",
        "Keep pushing down and keep your shoulders low.",
        "Switch legs halfway through the hold.",
    ),
)

private val dragonFlag = ExerciseGuide(
    cues = listOf(
        "Hold something solid behind your head and lift your body straight up onto your upper back.",
        "Lower in one straight line until you are just off the bench, then raise back up.",
        "Bend at the hips and it stops counting.",
    ),
    note = "Hold onto something that can't move: a heavy bench, a post or a bolted rail.",
)

private val wallPlank = ExerciseGuide(
    cues = listOf(
        "Start in a push-up position with your feet against a wall.",
        "Walk your feet up the wall until your body makes an L, hips over hands.",
        "Arms straight, shoulders pushed up towards your ears. Walk down slowly.",
    ),
)

private val crow = ExerciseGuide(
    cues = listOf(
        "Squat down, hands flat on the floor, knees resting on the backs of your upper arms.",
        "Lean forwards until your feet lift off.",
        "Look a little ahead of your hands, not at your feet.",
    ),
    note = "Put a cushion in front of you for the first tries.",
)

private val chestToWall = ExerciseGuide(
    cues = listOf(
        "Walk your feet up the wall until your chest is close to it and your body is straight.",
        "Push the floor away, arms locked, ears between your arms.",
        "Squeeze your legs together and point your toes.",
    ),
    note = "Practise coming down by walking your feet down the wall before your first long hold.",
)

private val toesOnWall = ExerciseGuide(
    cues = listOf(
        "From a chest-to-wall handstand, ease your toes off the wall a little at a time.",
        "Balance with your fingers: press them down when you tip forwards.",
        "Touch the wall again whenever you need to and keep going.",
    ),
)

private val freestanding = ExerciseGuide(
    cues = listOf(
        "Kick up with your hands a shoulder width apart, arms locked.",
        "Balance with your fingertips and the heels of your hands.",
        "Learn to come down by turning out of it, so falling never scares you.",
    ),
    note = "Clear space around you. Practise a few minutes most days rather than one long session.",
)

private val germanHang = ExerciseGuide(
    cues = listOf(
        "From a hang, tuck your knees and turn backwards through your arms.",
        "Let your feet drop towards the floor behind you and relax into the stretch.",
        "Come out the way you went in. Never force the bottom.",
    ),
    note = "Build the time up a few seconds a week. Your shoulders need weeks to get used to it.",
)

private val skinTheCat = ExerciseGuide(
    cues = listOf(
        "From a hang, tuck your knees and turn backwards through your arms to a German hang.",
        "Pull back through the same way, slowly, to the start.",
        "Keep your arms straight the whole way round.",
    ),
)

private val backLever = ExerciseGuide(
    cues = listOf(
        "From a German hang, lift your hips until your back is level with the floor, knees tucked.",
        "Arms straight, chest pushed slightly forwards.",
        "Hold, then lower back into the German hang under control.",
    ),
    note = "Straight-arm work loads the biceps tendon. Keep holds short and stop at any elbow pain.",
)

private val frontLever = ExerciseGuide(
    cues = listOf(
        "Hang with straight arms and pull the bar down towards your hips.",
        "Lift until your back is level with the floor, knees tucked to your chest.",
        "Squeeze your shoulder blades down and back. Don't let your hips sag.",
    ),
    note = "Straight-arm work loads the elbows. Keep holds short and stop at any elbow pain.",
)

private val plancheLean = ExerciseGuide(
    cues = listOf(
        "From a push-up position, fingers turned out, lean your shoulders forwards past your hands.",
        "Keep your arms locked and your upper back rounded.",
        "Lean further as it gets easier.",
    ),
    note = "Warm up your wrists first, every time.",
)

private val frogStand = ExerciseGuide(
    cues = listOf(
        "Squat down, hands flat, knees resting on the outside of your elbows.",
        "Lean forwards until your feet lift off, arms slightly bent.",
        "Hold still, then straighten your arms a little more each week.",
    ),
)

private val tuckPlanche = ExerciseGuide(
    cues = listOf(
        "Hands flat by your hips, arms locked, fingers turned out.",
        "Lean forwards and lift your feet with your knees tucked to your chest.",
        "Round your upper back and push the floor away.",
    ),
    note = "Warm up your wrists first, every time. Stop at any wrist or elbow pain.",
)

private val oneArmNegativePushUp = ExerciseGuide(
    cues = listOf(
        "Feet wide, one hand under your chest, the other behind your back.",
        "Lower as slowly as you can without twisting.",
        "Push back up with both hands and reset.",
    ),
)

private val oneArmHang = ExerciseGuide(
    cues = listOf(
        "Hang from one hand, arm straight, shoulder pulled down and in.",
        "Keep your body from spinning by tensing your core.",
        "Put the other hand back on the bar before your grip gives out.",
    ),
)

private val oneArmPullUp = ExerciseGuide(
    cues = listOf(
        "Hang from one hand, shoulder pulled down, body turned slightly into the arm.",
        "Pull until your chin is over the bar, legs still.",
        "Lower under control. Stop the set at any elbow pain.",
    ),
)

/** Guides for gates that aren't a ladder exercise under another name. */
internal val skillGuides: Map<String, ExerciseGuide> = mapOf(
    "first_push_up.negatives" to pushUpNegative,
    "first_dip.support" to supportHold,
    "first_dip.bench_dips" to benchDip,
    "pistol_squat.deep_squat" to deepSquat,
    "pistol_squat.assisted" to assistedPistol,
    "l_sit.one_leg" to oneLegLSit,
    "dragon_flag.dragon_flag" to dragonFlag,
    "handstand.wall_plank" to wallPlank,
    "handstand.crow" to crow,
    "handstand.chest_to_wall" to chestToWall,
    "handstand.toes_on_wall" to toesOnWall,
    "handstand.freestanding" to freestanding,
    "handstand.thirty" to freestanding,
    "handstand_push_up.wall_hold" to chestToWall,
    "back_lever.german_hang" to germanHang,
    "back_lever.skin_the_cat" to skinTheCat,
    "back_lever.tuck" to backLever,
    "back_lever.advanced_tuck" to backLever.leading("Hips open and back flat, knees still bent."),
    "back_lever.straddle" to backLever.leading("Legs straight and wide apart, body level behind the bar."),
    "back_lever.full" to backLever.leading("Legs straight and together, body level behind the bar."),
    "front_lever.tuck" to frontLever,
    "front_lever.advanced_tuck" to frontLever.leading("Back flat and hips open, knees still bent."),
    "front_lever.one_leg" to frontLever.leading("One leg out straight, the other tucked."),
    "front_lever.straddle" to frontLever.leading("Both legs straight and wide apart."),
    "front_lever.half_lay" to frontLever.leading("Legs together, hips straight, knees bent to 90 degrees."),
    "front_lever.full" to frontLever.leading("Legs straight and together, body level with the floor."),
    "tuck_planche.lean" to plancheLean,
    "tuck_planche.frog" to frogStand,
    "tuck_planche.five" to tuckPlanche,
    "tuck_planche.fifteen" to tuckPlanche,
    "one_arm_push_up.negatives" to oneArmNegativePushUp,
    "one_arm_pull_up.one_arm_hang" to oneArmHang,
    "one_arm_pull_up.one" to oneArmPullUp,
)

/** The same guide with the body position named first, for a harder version of a hold. */
private fun ExerciseGuide.leading(cue: String): ExerciseGuide = copy(cues = listOf(cue) + cues.drop(1))
