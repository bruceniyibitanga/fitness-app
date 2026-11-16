package app.levitx.core.coach

/*
 * Form cues for the harder end of each ladder: the variations past a strict
 * pull-up, dip, push-up, pistol, Nordic negative and hanging leg raise.
 * Kept apart from ExerciseGuides only to keep each file a readable length;
 * ExerciseGuides.forExercise looks in both.
 *
 * Cues follow the r/bodyweightfitness Recommended Routine and the Bodyweight
 * Fitness progressions chart, like the ladders themselves. They still need a
 * qualified coach's review before release.
 */

private val archerPullUp = ExerciseGuide(
    cues = listOf(
        "Hands twice shoulder width. Pull towards one hand while the other arm stays nearly straight.",
        "Chin over the bar beside the working hand, then lower under control.",
        "Alternate sides, or do every rep on one side before switching.",
    ),
    note = "The straight arm helps less as you get stronger. Keep it on the bar, not slack.",
)

private val typewriterPullUp = ExerciseGuide(
    cues = listOf(
        "Pull up wide, then slide across the bar from one hand to the other at the top.",
        "Keep your chin over the bar the whole way across.",
        "Lower from the far side, and start the next rep going the other way.",
    ),
)

private val oneArmNegative = ExerciseGuide(
    cues = listOf(
        "Get to the top with both hands, then let go with one.",
        "Lower as slowly as you can, shoulder pulled down, body turned slightly into the arm.",
        "Stop if your shoulder or elbow feels pinched. Step up and reset each rep.",
    ),
    note = "Hard on the elbow tendons. Keep the reps low and stop well before failure.",
)

private val archerRow = ExerciseGuide(
    cues = listOf(
        "Hands wide. Row towards one hand while the other arm stays straight.",
        "Body stays in one straight line from heels to head.",
        "Lower all the way, then switch sides.",
    ),
)

private val tuckFrontLeverRow = ExerciseGuide(
    cues = listOf(
        "Hang with straight arms, knees tucked to your chest and your back level with the floor.",
        "Pull the bar to your stomach without letting your hips drop.",
        "Lower to straight arms with the tuck held. Keep it slow.",
    ),
)

private val wallHandstandPushUp = ExerciseGuide(
    cues = listOf(
        "Kick up to a handstand facing away from the wall, hands a hand's width from it.",
        "Lower until your head lightly touches the floor, elbows pointing forwards.",
        "Press back to straight arms. Lock out and breathe at the top.",
    ),
    note = "Put a folded towel under your head the first few times.",
)

private val ringDip = ExerciseGuide(
    cues = listOf(
        "Start in a support hold with the rings close to your sides and arms locked.",
        "Lower until your shoulders are below your elbows, keeping the rings in.",
        "Press back up to straight arms without the rings drifting out.",
    ),
    note = "Hold a steady support for 30 seconds before your first ring dip.",
)

private val ringDipTurnedOut = ExerciseGuide(
    cues = listOf(
        "Same as a ring dip, but turn the rings out so your palms face forwards at the top.",
        "Keep them turned out while you lock out every rep.",
        "Lower and press under control. Don't bounce at the bottom.",
    ),
)

private val ringPushUp = ExerciseGuide(
    cues = listOf(
        "Rings a few centimetres off the floor, body in a straight line.",
        "Lower until your chest is level with your hands, rings close to your ribs.",
        "Press up and turn the rings out a little at the top.",
    ),
)

private val archerPushUp = ExerciseGuide(
    cues = listOf(
        "Hands wide. Lower towards one hand while the other arm slides out straight.",
        "Body stays rigid, hips level, chest to a fist off the floor.",
        "Press back up and switch sides.",
    ),
)

private val pseudoPlanchePushUp = ExerciseGuide(
    cues = listOf(
        "Hands by your waist, fingers turned out, shoulders leaning well past your hands.",
        "Keep the lean as you lower. Elbows stay close to your body.",
        "Press back to straight arms and round your upper back at the top.",
    ),
    note = "Build into it: the more you lean, the harder it gets. Warm up your wrists first.",
)

private val oneArmPushUp = ExerciseGuide(
    cues = listOf(
        "Feet wide, one hand under your chest, the other behind your back.",
        "Lower with your body square to the floor. Don't twist.",
        "Press up to a straight arm. Bring your feet closer as it gets easier.",
    ),
)

private val shrimpHandsForward = ExerciseGuide(
    cues = listOf(
        "Stand on one leg, the other knee bent behind you. Reach your arms forwards.",
        "Lower until your back knee touches a cushion, then stand back up.",
        "Keep your standing heel down and your knee over your toes.",
    ),
)

private val shrimpKneeDown = ExerciseGuide(
    cues = listOf(
        "Like the easier shrimp, but only the knee touches down. The back foot stays off the floor.",
        "Lean forwards from the hips to stay balanced.",
        "Drive up through the whole standing foot.",
    ),
)

private val shrimpHoldingFoot = ExerciseGuide(
    cues = listOf(
        "Hold your back foot behind you with the hand on the same side.",
        "Lower until your knee touches the floor, then stand up without letting go.",
        "Keep your chest up and your standing heel down.",
    ),
)

private val nordicCurl = ExerciseGuide(
    cues = listOf(
        "Kneel with your heels anchored, hips straight, body in one line.",
        "Lower forwards as slowly as you can, then pull yourself back up with your hamstrings.",
        "Push off the floor lightly with your hands if you need to at the bottom.",
    ),
    note = "Expect sore hamstrings for days after the first sessions. Start with fewer sets.",
)

private val toesToBar = ExerciseGuide(
    cues = listOf(
        "Hang with straight arms and your shoulders pulled down.",
        "Lift your legs, straight or nearly, until your toes touch the bar.",
        "Lower slowly and stop the swing before the next rep.",
    ),
)

private val lSit = ExerciseGuide(
    cues = listOf(
        "Hands beside your hips, arms locked, shoulders pushed down away from your ears.",
        "Lift your legs straight out in front, level with the floor.",
        "Point your toes and squeeze your thighs.",
    ),
    note = "Parallettes, two sturdy chairs or dip bars make it much easier to get off the floor.",
)

private val tuckDragonFlagNegative = ExerciseGuide(
    cues = listOf(
        "Lie on a bench or the floor and hold something solid behind your head.",
        "Roll up onto your upper back with your knees tucked, then lower as one piece.",
        "Your lower back stays off the floor until the very end.",
    ),
    note = "Hold onto something that can't move: a heavy bench, a post or a bolted rail.",
)

private val dragonFlagNegative = ExerciseGuide(
    cues = listOf(
        "Like the tuck version, but with your legs straight.",
        "Lower your body in a straight line from shoulders to toes.",
        "Bend at the hips and it stops counting. Tuck again if it does.",
    ),
    note = "Hold onto something that can't move: a heavy bench, a post or a bolted rail.",
)

/** The guide for every exercise in the harder end of each ladder. */
internal val advancedGuides: Map<String, ExerciseGuide> = mapOf(
    "vertical_pull.archer" to archerPullUp,
    "vertical_pull.typewriter" to typewriterPullUp,
    "vertical_pull.one_arm_negative" to oneArmNegative,
    "horizontal_pull.archer_row" to archerRow,
    "horizontal_pull.ring_archer_row" to archerRow,
    "horizontal_pull.tuck_front_lever_row" to tuckFrontLeverRow,
    "horizontal_pull.ring_tuck_front_lever_row" to tuckFrontLeverRow,
    "vertical_push.wall_hspu" to wallHandstandPushUp,
    "vertical_push.ring_dip" to ringDip,
    "vertical_push.ring_dip_turned_out" to ringDipTurnedOut,
    "horizontal_push.ring" to ringPushUp,
    "horizontal_push.archer" to archerPushUp,
    "horizontal_push.pseudo_planche" to pseudoPlanchePushUp,
    "horizontal_push.one_arm" to oneArmPushUp,
    "squat.beginner_shrimp" to shrimpHandsForward,
    "squat.intermediate_shrimp" to shrimpKneeDown,
    "squat.shrimp" to shrimpHoldingFoot,
    "hinge.nordic" to nordicCurl,
    "core.toes_to_bar" to toesToBar,
    "core.l_sit" to lSit,
    "core.tuck_dragon_flag_negative" to tuckDragonFlagNegative,
    "core.dragon_flag_negative" to dragonFlagNegative,
)
