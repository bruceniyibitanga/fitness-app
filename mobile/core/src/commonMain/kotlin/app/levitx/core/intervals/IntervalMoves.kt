package app.levitx.core.intervals

import app.levitx.core.coach.Equipment
import app.levitx.core.coach.Exercise
import app.levitx.core.coach.ExerciseGuide
import app.levitx.core.coach.MovementPattern
import app.levitx.core.skills.GateMeasure

/*
 * The moves interval workouts are built from. Each is an Exercise so history
 * can name it, measured in seconds, with a cardio or mobility pattern so the
 * coach never counts it. Their form cues are below. Like the rest of the
 * catalogue, they need a qualified coach's review before release.
 */

/** Seconds any interval move can be done for. Declared first: the moves below use it as they're built. */
private val TIMED_RANGE = 10..600

private fun cardio(id: String, name: String, equipment: Set<Equipment> = emptySet()) =
    Exercise("cardio.$id", name, MovementPattern.CARDIO, GateMeasure.HOLD_SECONDS, TIMED_RANGE, equipment)

private fun stretch(id: String, name: String, perSide: Boolean = false) =
    Exercise("mobility.$id", name, MovementPattern.MOBILITY, GateMeasure.HOLD_SECONDS, TIMED_RANGE, isPerSide = perSide)

private val rope = setOf(Equipment.JUMP_ROPE)

internal val marching = cardio("march", "Marching on the spot")
internal val jumpingJacks = cardio("jumping_jacks", "Jumping jacks")
internal val burpees = cardio("burpees", "Burpees")
internal val mountainClimbers = cardio("mountain_climbers", "Mountain climbers")
internal val squatJumps = cardio("squat_jumps", "Squat jumps")
internal val highKnees = cardio("high_knees", "High knees")
internal val hardMinute = cardio("hard_minute", "Hard minute")
internal val ropeEasy = cardio("rope_easy", "Skipping, easy", rope)
internal val ropeFast = cardio("rope_fast", "Skipping, fast", rope)

internal val catCow = stretch("cat_cow", "Cat-cow")
internal val worldsGreatest = stretch("worlds_greatest", "World's greatest stretch", perSide = true)
internal val openBook = stretch("open_book", "Open book", perSide = true)
internal val hipFlexor = stretch("hip_flexor", "Kneeling hip flexor stretch", perSide = true)
internal val couchStretch = stretch("couch_stretch", "Couch stretch", perSide = true)
internal val pigeon = stretch("pigeon", "Pigeon stretch", perSide = true)
internal val hamstring = stretch("hamstring", "Standing hamstring stretch", perSide = true)
internal val frog = stretch("frog", "Frog stretch")
internal val deepSquat = stretch("deep_squat", "Deep squat hold")
internal val wrists = stretch("wrists", "Wrist stretch")
internal val passThrough = stretch("pass_through", "Shoulder pass-through")
internal val doorwayChest = stretch("doorway_chest", "Doorway chest stretch")
internal val threadNeedle = stretch("thread_needle", "Thread the needle", perSide = true)
internal val childsPose = stretch("childs_pose", "Child's pose")

/** Every interval move. */
internal val intervalMoves: List<Exercise> = listOf(
    marching, jumpingJacks, burpees, mountainClimbers, squatJumps, highKnees, hardMinute, ropeEasy, ropeFast,
    catCow, worldsGreatest, openBook, hipFlexor, couchStretch, pigeon, hamstring, frog, deepSquat, wrists,
    passThrough, doorwayChest, threadNeedle, childsPose,
)

private fun guide(vararg cues: String, note: String? = null) = ExerciseGuide(cues = cues.toList(), note = note)

/** Form cues for every interval move, by id. */
internal val intervalGuides: Map<String, ExerciseGuide> = mapOf(
    marching.id to
        guide("Lift your knees to hip height and swing your arms.", "Easy pace: you could hold a conversation."),
    jumpingJacks.id to guide("Jump your feet wide as your arms go overhead.", "Land softly on the balls of your feet."),
    burpees.id to guide(
        "Squat, put your hands down and jump your feet back to a push-up position.",
        "Jump your feet in and jump up with your arms overhead.",
        note = "Step back and forward instead of jumping to make it easier.",
    ),
    mountainClimbers.id to guide(
        "From a push-up position, drive your knees towards your chest one at a time.",
        "Keep your hips level with your shoulders.",
    ),
    squatJumps.id to
        guide(
            "Squat to thigh level, then jump as high as you can.",
            "Land softly and sink straight into the next one.",
        ),
    highKnees.id to guide(
        "Run on the spot, knees to hip height.",
        "Stay on the balls of your feet and pump your arms.",
    ),
    hardMinute.id to guide(
        "Burpees, high knees or squat jumps: whatever you can keep hard for the full minute.",
        "Hard means you can only say a word or two.",
        note = "Ease the pace rather than stop. The rest is for recovering.",
    ),
    ropeEasy.id to
        guide(
            "Small, easy bounces on the balls of your feet.",
            "Turn the rope with your wrists, elbows by your sides.",
        ),
    ropeFast.id to guide("As fast as you can turn it cleanly.", "If you trip, pick it up and keep going."),
    catCow.id to guide(
        "On hands and knees, round your back up towards the ceiling.",
        "Then let your belly drop and lift your chest. Move slowly with your breath.",
    ),
    worldsGreatest.id to guide(
        "Step into a long lunge and put both hands inside your front foot.",
        "Turn your chest towards the front knee and reach that arm to the ceiling.",
    ),
    openBook.id to guide(
        "Lie on your side, knees bent, arms straight out in front.",
        "Open the top arm across to the other side, following it with your eyes.",
    ),
    hipFlexor.id to guide(
        "Kneel on one knee, the other foot flat in front.",
        "Squeeze the glute of the back leg and shift your hips forwards until the front of the hip stretches.",
    ),
    couchStretch.id to guide(
        "Kneel with your back foot up against a wall or sofa behind you.",
        "Bring your front foot forwards and lift your chest slowly.",
        note = "Pad the knee underneath.",
    ),
    pigeon.id to guide(
        "From hands and knees, bring one knee forwards behind your wrist, shin across the body.",
        "Slide the back leg straight behind you and sink your hips.",
        note = "Too much? Do it lying on your back with your ankle on the other knee.",
    ),
    hamstring.id to guide(
        "Put one heel on a low step, leg straight.",
        "Hinge forwards from the hips with a flat back until you feel the back of the leg.",
    ),
    frog.id to guide(
        "On your forearms, spread your knees wide with your feet in line with them.",
        "Rock your hips back slowly. Ease off if your knees complain.",
    ),
    deepSquat.id to guide(
        "Feet shoulder width, heels down, sink as low as you can.",
        "Push your knees out with your elbows and keep your chest up.",
    ),
    wrists.id to guide(
        "On hands and knees, turn your fingers to point back at your knees.",
        "Lean back gently, then turn your hands over and do the same on the backs.",
        note = "Do this before every handstand or planche session.",
    ),
    passThrough.id to guide(
        "Hold a towel or band wide with straight arms.",
        "Lift it over your head and down behind you, then back. Go wider if it pinches.",
    ),
    doorwayChest.id to guide(
        "Forearms on either side of a doorway, elbows at shoulder height.",
        "Step through until you feel the stretch across your chest.",
    ),
    threadNeedle.id to guide(
        "On hands and knees, slide one arm under your body along the floor.",
        "Rest that shoulder and the side of your head down, then come back up and reach to the ceiling.",
    ),
    childsPose.id to
        guide("Kneel and sit back on your heels.", "Reach your arms forwards and let your forehead rest on the floor."),
)
