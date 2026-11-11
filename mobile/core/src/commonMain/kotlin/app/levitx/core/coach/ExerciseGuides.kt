package app.levitx.core.coach

/**
 * How to do an exercise well: a few form cues, and a note when it needs
 * something the kit list doesn't cover or has a common way to go wrong.
 *
 * @property cues Two or three short form cues, most important first.
 * @property note Set-up or safety advice, or null when there's nothing to add.
 */
public data class ExerciseGuide(val cues: List<String>, val note: String? = null)

/**
 * The guide for every exercise the coach can plan, in English.
 *
 * Kept in `:core` beside the catalogue, like [CoachPhrases], so the phone and
 * the watch say the same thing and a test can check no exercise is missing
 * one. The cues are standard coaching points for each movement. Like the
 * catalogue, they still need a qualified coach's review before release.
 */
public object ExerciseGuides {
    private val hang = ExerciseGuide(
        cues = listOf(
            "Grip just wider than your shoulders, arms fully straight.",
            "Keep your shoulders active: don't let them creep up to your ears.",
            "Breathe slowly and keep your legs still.",
        ),
    )

    private val scapularPull = ExerciseGuide(
        cues = listOf(
            "From a straight-arm hang, pull your shoulder blades down and back.",
            "Your body rises a few centimetres. Your elbows stay straight.",
            "Lower under control back to the hang.",
        ),
    )

    private val negativePull = ExerciseGuide(
        cues = listOf(
            "Jump or step up so your chin is over the bar.",
            "Lower for 3 to 5 seconds, all the way to straight arms.",
            "Step back up rather than pulling up again.",
        ),
    )

    private val pullUp = ExerciseGuide(
        cues = listOf(
            "Start from straight arms with your shoulders pulled down.",
            "Pull your chest towards the bar until your chin is over it.",
            "Lower all the way, under control. No kicking or swinging.",
        ),
    )

    private val chestToBar = ExerciseGuide(
        cues = listOf(
            "Pull higher than a normal pull-up, until your chest touches the bar.",
            "Lean back slightly at the top and drive your elbows down.",
            "Lower all the way to straight arms.",
        ),
    )

    private val row = ExerciseGuide(
        cues = listOf(
            "Body in one straight line from head to heels, hips up.",
            "Pull your chest to the bar or rings, squeezing your shoulder blades together.",
            "Lower until your arms are straight.",
        ),
        note = "Make it easier by standing more upright, harder by walking your feet forward.",
    )

    private val pike = ExerciseGuide(
        cues = listOf(
            "Hips high, so your body makes an upside-down V.",
            "Lower the top of your head towards the floor in front of your hands.",
            "Elbows track back at about 45 degrees, not flared out.",
        ),
        note = "Wrists or shoulders pinching? Use push-up handles or fists, or go back a rung.",
    )

    private val negativeDip = ExerciseGuide(
        cues = listOf(
            "Start at the top with straight arms, shoulders pulled down.",
            "Lower for 3 to 5 seconds until your shoulders are just below your elbows.",
            "Step back up rather than pressing.",
        ),
        note = "Stop short of the bottom if the front of your shoulders hurts.",
    )

    private val dip = ExerciseGuide(
        cues = listOf(
            "Lean forward slightly, shoulders pulled down, away from your ears.",
            "Lower until your shoulders are just below your elbows.",
            "Press back up to straight arms.",
        ),
        note = "Stop short of the bottom if the front of your shoulders hurts.",
    )

    private val wallNegative = ExerciseGuide(
        cues = listOf(
            "Kick up into a handstand with your back to the wall.",
            "Lower your head slowly towards the floor, about 3 seconds.",
            "Come down from the wall when your head touches the floor.",
        ),
        note = "Put a folded towel under your head, and only try this once wall handstands feel steady.",
    )

    private val pushUp = ExerciseGuide(
        cues = listOf(
            "Hands under your shoulders, body in one straight line.",
            "Lower until your chest is a fist's width from the floor or box.",
            "Elbows at about 45 degrees from your body, not flared out.",
        ),
    )

    private val wallPushUp = ExerciseGuide(
        cues = listOf(
            "Hands on the wall at shoulder height, a little wider than your shoulders.",
            "Step your feet back until you lean in a straight line from heels to head.",
            "Bend your elbows until your nose nearly touches the wall, then press back.",
        ),
        note = "Step further from the wall to make it harder.",
    )

    private val kneePushUp = ExerciseGuide(
        cues = listOf(
            "Knees down, but keep a straight line from knees to head.",
            "Lower until your chest is a fist's width from the floor.",
            "Elbows at about 45 degrees from your body.",
        ),
    )

    private val closeGrip = ExerciseGuide(
        cues = listOf(
            "Hands just inside shoulder width.",
            "Keep your elbows tucked close to your sides on the way down.",
            "Body in one straight line throughout.",
        ),
    )

    private val boxSquat = ExerciseGuide(
        cues = listOf(
            "Feet shoulder width apart in front of a chair.",
            "Sit back until you just touch the chair, then stand without rocking.",
            "Knees follow your toes, chest up.",
        ),
    )

    private val squat = ExerciseGuide(
        cues = listOf(
            "Feet about shoulder width, toes turned out a little.",
            "Sit down between your heels as deep as you can with a flat back.",
            "Knees follow your toes, heels stay down.",
        ),
    )

    private val splitSquat = ExerciseGuide(
        cues = listOf(
            "Long stance, back heel up.",
            "Drop your back knee straight down towards the floor.",
            "Push through the front heel to stand.",
        ),
    )

    private val reverseLunge = ExerciseGuide(
        cues = listOf(
            "Step back far enough that both knees bend to about 90 degrees.",
            "Back knee lightly touches or nearly touches the floor.",
            "Push through the front heel to step back up.",
        ),
    )

    private val bulgarian = ExerciseGuide(
        cues = listOf(
            "Rear foot on a chair or bench, front foot well forward.",
            "Lower straight down until your front thigh is about level.",
            "Front knee follows your toes. Push through the front heel.",
        ),
        note = "Needs a chair, bench or step about knee height.",
    )

    private val stepUp = ExerciseGuide(
        cues = listOf(
            "Whole foot on a step or bench about knee height.",
            "Stand up using the top leg only. Don't push off the bottom foot.",
            "Step down slowly.",
        ),
        note = "Needs a sturdy step, bench or chair about knee height.",
    )

    private val pistolToChair = ExerciseGuide(
        cues = listOf(
            "Stand on one leg in front of a chair, other leg out in front.",
            "Sit back slowly until you touch the chair.",
            "Stand back up on the one leg without rocking.",
        ),
    )

    private val pistol = ExerciseGuide(
        cues = listOf(
            "One leg out in front, arms forward for balance.",
            "Sit all the way down on the standing leg, heel flat.",
            "Stand up without letting the knee cave in.",
        ),
        note = "Hold a door frame lightly at first if balance is what stops you.",
    )

    private val bridge = ExerciseGuide(
        cues = listOf(
            "Lie on your back, knees bent, feet flat near your hips.",
            "Push through your heels and squeeze your glutes to lift your hips.",
            "Pause at the top without arching your lower back.",
        ),
    )

    private val singleLegBridge = ExerciseGuide(
        cues = listOf(
            "One foot down, the other leg straight or knee hugged in.",
            "Push through the heel and lift your hips level.",
            "Keep your hips square: don't let one side drop.",
        ),
    )

    private val hipThrust = ExerciseGuide(
        cues = listOf(
            "Upper back on a sofa or bench edge, feet flat.",
            "Drive your hips up until your body is flat from shoulders to knees.",
            "Chin tucked, ribs down, squeeze your glutes at the top.",
        ),
        note = "Needs a sofa, bench or bed edge about knee height.",
    )

    private val singleLegDeadlift = ExerciseGuide(
        cues = listOf(
            "Stand on one leg with a soft knee.",
            "Hinge at the hips, reaching the other leg back as your chest lowers.",
            "Back flat, hips square to the floor. Stand back up through the heel.",
        ),
        note = "Touch a wall with one hand if balance is what limits you.",
    )

    private val slidingCurl = ExerciseGuide(
        cues = listOf(
            "Lie on your back with heels on towels or sliders, hips lifted.",
            "Pull your heels towards your hips while keeping the hips up.",
            "Slide back out slowly.",
        ),
        note = "Needs a smooth floor and towels or sliders under your heels.",
    )

    private val nordic = ExerciseGuide(
        cues = listOf(
            "Kneel with your heels hooked under something that won't move.",
            "Keep a straight line from knees to head and lower forward as slowly as you can.",
            "Catch yourself with your hands, then push back up.",
        ),
        note = "Needs something solid to hook your heels under, like a heavy couch, " +
            "or a partner holding your ankles. Put a cushion under your knees.",
    )

    private val plank = ExerciseGuide(
        cues = listOf(
            "Forearms under your shoulders, body in one straight line.",
            "Squeeze your glutes and brace like you're about to be poked in the stomach.",
            "Don't let your hips sag or pike up.",
        ),
    )

    private val deadBug = ExerciseGuide(
        cues = listOf(
            "On your back, arms up, knees bent at 90 degrees above your hips.",
            "Lower the opposite arm and leg slowly, keeping your lower back on the floor.",
            "Return and switch sides.",
        ),
    )

    private val hollow = ExerciseGuide(
        cues = listOf(
            "Lower back pressed into the floor the whole time.",
            "Lift your shoulders and legs, arms reaching past your ears.",
            "Bend your knees to make it easier. Never let your lower back lift.",
        ),
    )

    private val lyingLegRaise = ExerciseGuide(
        cues = listOf(
            "Lie flat, hands under your hips, legs straight.",
            "Lift your legs to vertical, keeping your lower back down.",
            "Lower slowly and stop before your back arches.",
        ),
    )

    private val hangingKneeRaise = ExerciseGuide(
        cues = listOf(
            "Hang with your shoulders active and your body still.",
            "Bring your knees up to your chest by curling your hips.",
            "Lower slowly without swinging.",
        ),
    )

    private val tuckLSit = ExerciseGuide(
        cues = listOf(
            "Hands beside your hips, arms locked straight.",
            "Push down to lift your hips and bring your knees to your chest.",
            "Shoulders pushed down, away from your ears.",
        ),
        note = "Much easier on dip bars, push-up handles or two sturdy chairs than flat on the floor.",
    )

    private val hangingLegRaise = ExerciseGuide(
        cues = listOf(
            "Hang with your shoulders active.",
            "Lift straight legs to at least hip height by curling your hips.",
            "Lower slowly without swinging.",
        ),
    )

    private val falseGripHang = ExerciseGuide(
        cues = listOf(
            "Rest the heel of your palm on top of the bar, wrists bent over it.",
            "Hang with straight arms and active shoulders.",
            "Build up time slowly: the wrists need a few weeks.",
        ),
    )

    private val chinOverBar = ExerciseGuide(
        cues = listOf(
            "Jump or step up so your chin is over the bar.",
            "Hold there with your elbows tight to your sides.",
            "Lower slowly when you can't hold it any longer.",
        ),
    )

    private val straightBarDip = ExerciseGuide(
        cues = listOf(
            "Start above the bar with straight arms, bar at your hips.",
            "Lean forward and lower until the bar touches your lower chest.",
            "Press back up to straight arms.",
        ),
    )

    private val muscleUp = ExerciseGuide(
        cues = listOf(
            "Use a false grip and pull explosively to your lower chest.",
            "Lean your chest over the bar as your elbows come up.",
            "Press out to straight arms on top.",
        ),
        note = "Learn it strict before adding any kip. A kipping muscle-up before you have the strength is hard on " +
            "shoulders and elbows.",
    )

    private val byId: Map<String, ExerciseGuide> = mapOf(
        "vertical_pull.dead_hang" to hang,
        "vertical_pull.scapular_pull_up" to scapularPull,
        "vertical_pull.negative" to negativePull,
        "vertical_pull.band_pull_up" to pullUp.withNote("Loop the band over the bar and put one foot or knee in it."),
        "vertical_pull.pull_up" to pullUp,
        "vertical_pull.chin_up" to pullUp.startingWith("Palms facing you, hands shoulder width."),
        "vertical_pull.chest_to_bar" to chestToBar,
        "vertical_pull.wide_pull_up" to pullUp.startingWith("Hands well wider than your shoulders."),
        "horizontal_pull.row_knees_bent" to row,
        "horizontal_pull.ring_row_upright" to row,
        "horizontal_pull.band_row" to ExerciseGuide(
            cues = listOf(
                "Sit tall with legs straight and the band around your feet.",
                "Pull your hands to your lower ribs, squeezing your shoulder blades together.",
                "Let the band pull your arms straight again slowly.",
            ),
        ),
        "horizontal_pull.row" to row,
        "horizontal_pull.ring_row" to row,
        "horizontal_pull.row_feet_raised" to row,
        "horizontal_pull.ring_row_feet_raised" to row,
        "vertical_push.pike_hands_raised" to pike,
        "vertical_push.pike" to pike,
        "vertical_push.pike_feet_raised" to pike,
        "vertical_push.negative_dip" to negativeDip,
        "vertical_push.dip" to dip,
        "vertical_push.wall_negative" to wallNegative,
        "horizontal_push.wall" to wallPushUp,
        "horizontal_push.hands_61cm" to pushUp,
        "horizontal_push.knees" to kneePushUp,
        "horizontal_push.hands_30cm" to pushUp,
        "horizontal_push.push_up" to pushUp,
        "horizontal_push.close_grip" to closeGrip,
        "horizontal_push.feet_30cm" to pushUp,
        "horizontal_push.feet_61cm" to pushUp,
        "squat.box" to boxSquat,
        "squat.bodyweight" to squat,
        "squat.split" to splitSquat,
        "squat.reverse_lunge" to reverseLunge,
        "squat.bulgarian" to bulgarian,
        "squat.step_up" to stepUp,
        "squat.pistol_to_chair" to pistolToChair,
        "squat.pistol" to pistol,
        "hinge.glute_bridge" to bridge,
        "hinge.single_leg_bridge" to singleLegBridge,
        "hinge.hip_thrust" to hipThrust,
        "hinge.single_leg_deadlift" to singleLegDeadlift,
        "hinge.sliding_curl" to slidingCurl,
        "hinge.nordic_negative" to nordic,
        "core.plank" to plank,
        "core.dead_bug" to deadBug,
        "core.hollow_hold" to hollow,
        "core.lying_leg_raise" to lyingLegRaise,
        "core.hanging_knee_raise" to hangingKneeRaise,
        "core.tuck_l_sit" to tuckLSit,
        "core.hanging_leg_raise" to hangingLegRaise,
        // Skill gates are trained under the gate's own id.
        "first_pull_up.dead_hang" to hang,
        "first_pull_up.scapular_pulls" to scapularPull,
        "first_pull_up.flexed_arm_hang" to chinOverBar,
        "first_pull_up.negatives" to negativePull,
        "first_pull_up.pull_up" to pullUp,
        "muscle_up.dead_hang" to hang,
        "muscle_up.dips" to straightBarDip,
        "muscle_up.pull_ups" to pullUp,
        "muscle_up.chest_to_bar" to chestToBar,
        "muscle_up.false_grip_hang" to falseGripHang,
        "muscle_up.muscle_up" to muscleUp,
    )

    /** The guide for the exercise with [exerciseId], or null when there isn't one. */
    public fun forExercise(exerciseId: String): ExerciseGuide? = byId[exerciseId]
}

/** The same guide with its first cue replaced, for a variation that differs only in how it starts. */
private fun ExerciseGuide.startingWith(cue: String): ExerciseGuide = copy(cues = listOf(cue) + cues.drop(1))

/** The same guide with [note] added. */
private fun ExerciseGuide.withNote(note: String): ExerciseGuide = copy(note = note)
