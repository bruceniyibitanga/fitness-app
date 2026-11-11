package app.levitx.core.coach

import app.levitx.core.pain.PainAdvice
import app.levitx.core.pain.PainAssessment
import app.levitx.core.pain.PainReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Runs made-up people through twelve weeks with the coach and checks the plans
 * look like what a careful trainer would do (docs/app/COACH-ENGINE.md, section 8).
 *
 * The safety checks here are worked out from the sessions themselves, not by
 * asking the hard rules again, so a rule that is wrong can't vouch for itself.
 */
class CoachSimulationTest {
    private val weeks = 12
    private val monWedFri = trainingDays(weeks, 0, 2, 4)

    private fun beginner() = SimulatedAthlete.even(strength = 1.3, skill = 0.4, gain = 0.08)

    @Test
    fun aBeginnerProgressesAtABelievablePace() {
        val (model, log) = simulate(newcomer(), beginner(), monWedFri)
        assertSafe(log)

        val gatesPassed = assertNotNull(model.profile.goalPath).let {
            it.gates.size -
                it.progressFor(model.gateBests).gatesRemaining
        }
        assertTrue(gatesPassed in 1..3, "passed $gatesPassed gates in twelve weeks")
        val firstGatePassedOn = log.first { it.model.gateBests.values.any { best -> best >= 30 } }.day
        assertTrue(firstGatePassedOn >= 10, "the first gate fell on day $firstGatePassedOn")
        assertTrue(model.rungs.values.count { it > 0 } >= 4, "most ladders moved up: ${model.rungs}")
        assertTrue(log.all { it.done != null }, "a steady beginner is never left without a session")
    }

    @Test
    fun aSteadyBeginnerIsRarelyAskedToEaseOff() {
        val (_, log) = simulate(newcomer(), beginner(), monWedFri)
        val unplanned = log.mapNotNull { it.recommendation.suggestion }
            .filterNot { it.signals.single() is ReadinessSignal.LighterWeek }
        assertTrue(unplanned.size <= 2, "asked ${unplanned.size} times: ${unplanned.map { it.signals }}")
    }

    @Test
    fun everyBlockEndsInALighterWeekAndTheNextBringsNewExercises() {
        val (model, log) = simulate(newcomer(), beginner(), monWedFri)
        val lighterDays = log.filter { it.recommendation.suggestion?.signals?.single() is ReadinessSignal.LighterWeek }
        assertEquals(6, lighterDays.size, "two blocks, three lighter sessions each")
        assertTrue(lighterDays.all { assertNotNull(it.done).isLight })
        assertEquals(2, model.block.index)
        val introduced = log.map { it.recommendation.reason }.filterIsInstance<CoachReason.NewThisBlock>()
        assertTrue(introduced.isNotEmpty(), "new exercises are pointed out")
    }

    @Test
    fun sessionsVaryWithoutChangingTheSkillWork() {
        val (_, log) = simulate(newcomer(), beginner(), monWedFri)
        val sessions = log.mapNotNull { it.done }.filterNot { it.isLight }
        assertTrue(sessions.all { it.plans.first().gateId != null }, "the skill always comes first")
        val mixes = sessions.map { session -> session.plans.drop(1).map { it.exercise.pattern } }
        assertTrue(mixes.toSet().size >= 3, "more than one kind of session")
        assertTrue(
            mixes.zipWithNext().count { (a, b) ->
                a == b
            } < mixes.size / 2,
            "not the same session over and over",
        )
    }

    @Test
    fun aPlateauEndsInEasingBackNotInEverHarderTargets() {
        val stuck = SimulatedAthlete.even(strength = 2.5, skill = 1.5, gain = 0.0)
        val (model, log) = simulate(newcomer(), stuck, monWedFri)
        assertSafe(log)
        val missed = log.count { day ->
            day.done?.plans.orEmpty().any { it.isHard && it.gateId == null && it.workTarget > capacityOf(it, 2.5) }
        }
        assertTrue(missed <= log.size / 3, "asked for more than they could do in $missed of ${log.size} sessions")
        assertTrue(
            model.exercises.values.any { state ->
                state.marks.any { !it.hitTarget }
            },
            "they did hit their limit",
        )
    }

    @Test
    fun aBadSpellBringsASuggestionAndTheCoachBacksOff() {
        val athlete = beginner()
        val badDays = 42..55
        val (_, log) = simulate(newcomer(), athlete, monWedFri) { day, model ->
            athlete.capacityFactor = if (day in badDays) 0.6 else 1.0
            model
        }
        assertSafe(log)
        val asked = log.filter { it.day in 42..62 }.mapNotNull { it.recommendation.suggestion }
            .filter { suggestion -> suggestion.signals.any { it is ReadinessSignal.PerformanceDrop } }
        assertTrue(asked.isNotEmpty(), "falling results that feel hard are noticed")
    }

    @Test
    fun someoneWhoTrainsOnceAWeekIsNeverPunishedForIt() {
        val (model, log) = simulate(newcomer(), beginner(), trainingDays(weeks, 0))
        assertSafe(log)
        assertTrue(log.all { it.done != null && it.recommendation.suggestion == null })
        assertTrue(log.all { assertNotNull(it.done).plans.first().gateId != null }, "the goal still comes first")
        assertEquals(0, model.block.index, "twelve sessions don't make a block, so no lighter week yet")
        assertTrue(model.gateBests.values.max() > 12, "and they still improve")
    }

    @Test
    fun aComebackAfterThreeWeeksOffStartsLighter() {
        val days = trainingDays(3, 0, 2, 4) + trainingDays(3, 0, 2, 4).map { it + 42 }
        val (_, log) = simulate(newcomer(), beginner(), days)
        assertSafe(log)
        val back = log.first { it.day == 42 }
        val suggestion = assertNotNull(back.recommendation.suggestion)
        assertIs<ReadinessSignal.LongGap>(suggestion.signals.single())
        assertTrue(assertNotNull(back.done).isLight)
        val next = log.first { it.day == 44 }
        assertTrue(next.recommendation.suggestion == null && !assertNotNull(next.done).isLight, "then back to normal")
    }

    @Test
    fun fastWeightLossHoldsProgressAndIsMentionedOnce() {
        var notices = 0

        // Steady at 80 kg for four weeks, then losing 1.5% a week for five, then steady again.
        fun weightOn(day: Int): Double = 80.0 - 0.17 * (day.coerceIn(28, 63) - 28)
        val athlete = SimulatedAthlete.even(strength = 1.3, skill = 0.4, gain = 0.08)
        val (_, log) = simulate(newcomer(), athlete, monWedFri) { day, model ->
            val weighed = if (day % DAYS_PER_WEEK == 0) model.withWeighIn(WeighIn(atDay(day), weightOn(day))) else model
            val told = Coach().recommend(weighed, atDay(day)).notices.isNotEmpty()
            if (told) notices++
            if (told) weighed.withWeightNoticeShown() else weighed
        }
        assertSafe(log)
        assertEquals(1, notices)

        val held = log.filter { contextOn(it.day, it.model).holdProgress }
        assertTrue(held.size >= 6, "the spell was noticed: ${held.map { it.day }}")
        val ladderTargets = held.map { day -> day.model.exercises.filterKeys { ExerciseCatalogue.byId(it) != null } }
        val raised = ladderTargets.zipWithNext().sumOf { (before, after) ->
            after.count { (id, state) -> !state.isCalibrating && state.target > (before[id]?.target ?: Int.MAX_VALUE) }
        }
        assertEquals(0, raised, "no target went up while weight was falling fast")
        assertTrue(
            log.last().model.exercises.values.any {
                it.note == ProgressNote.TARGET_UP
            },
            "progress resumes after",
        )
    }

    @Test
    fun aSoreElbowIsWorkedAroundUntilItSettles() {
        val sore = PainAssessment(PainAdvice.EASE_OFF, PainReason.WORSE_NEXT_MORNING)
        val fine = PainAssessment(PainAdvice.CONTINUE_AS_PLANNED, PainReason.WITHIN_GUIDE)
        val (_, log) = simulate(newcomer(), beginner(), monWedFri) { day, model ->
            when (day) {
                14 -> model.withPain(BodyArea.ELBOW, sore)
                28 -> model.withPain(BodyArea.ELBOW, fine)
                else -> model
            }
        }
        assertSafe(log)
        val during = log.filter { it.day in 14..27 }
        assertTrue(during.all { it.recommendation.reason == CoachReason.WorkingAround(BodyArea.ELBOW) })
        assertTrue(during.all { day -> assertNotNull(day.done).plans.any { it.isHard } }, "legs and core carry on")
        val after = log.first { it.day == 28 }
        assertTrue(assertNotNull(after.done).plans.any { it.isHard && BodyArea.ELBOW in it.exercise.pattern.loads })
    }

    @Test
    fun shortSessionsAndExtraDaysStaySafe() {
        val fiveDays = trainingDays(6, 0, 1, 2, 4, 5)
        val profile = pullUpProfile(sessionsPerWeek = 5, sessionMinutes = 20)
        val (_, log) = simulate(newcomer(profile), beginner(), fiveDays)
        assertSafe(log)
        assertTrue(log.all { (it.done?.estimatedMinutes ?: 0) <= 21 })
    }

    @Test
    fun aMuscleUpPlanAndAPlanWithNoGoalStaySafeToo() {
        val muscleUp = newcomer(CoachProfile("muscle_up", equipment = fullKit))
        assertSafe(simulate(muscleUp, SimulatedAthlete.even(3.2, 0.5, 0.08), monWedFri).second)
        val general = newcomer(CoachProfile(goalPathId = null, sessionsPerWeek = 4, equipment = fullKit))
        assertSafe(simulate(general, beginner(), trainingDays(weeks, 0, 1, 3, 5)).second)
    }

    /** What the simulated person can do in a set of [plan] at [strength], as [SimulatedAthlete] works it out. */
    private fun capacityOf(plan: ExercisePlan, strength: Double): Int {
        val rung = ExerciseCatalogue.ladder(plan.exercise.pattern).rungs.indexOfFirst { plan.exercise in it.options }
        val range = plan.exercise.range
        return (range.first + (strength - rung) * (range.last - range.first)).toInt()
    }

    /**
     * Checks every session done against the safety limits, worked out from
     * the log alone: rest between hard days, the weekly ceiling, pain, gate
     * order and the time the person has.
     */
    private fun assertSafe(log: List<SimulatedDay>) {
        val hardDays = mutableMapOf<MovementPattern, MutableList<Pair<Int, Int>>>()
        log.forEach { day ->
            val session = day.done ?: return@forEach
            session.directHardSets.forEach { (pattern, sets) ->
                val earlier = hardDays.getOrPut(pattern) { mutableListOf() }
                val lastHardDay = earlier.lastOrNull { it.second >= CoachTuning.HARD_SESSION_MIN_SETS }?.first
                assertTrue(
                    lastHardDay == null || day.day - lastHardDay >= 2,
                    "$pattern trained hard on day $lastHardDay and again on day ${day.day}",
                )
                val thisWeek = earlier.filter { it.first > day.day - DAYS_PER_WEEK }.sumOf { it.second } + sets
                assertTrue(
                    thisWeek <= CoachTuning.MAX_WEEKLY_HARD_SETS,
                    "$pattern reached $thisWeek hard sets in the week to day ${day.day}",
                )
                earlier += day.day to sets
            }
            assertPainRespected(day, session)
            val currentGate = CoachContext(day.model, atDay(day.day)).currentGate?.id
            assertTrue(
                session.plans.mapNotNull {
                    it.gateId
                }.all { it == currentGate },
                "gate out of order on day ${day.day}",
            )
            assertTrue(
                session.estimatedMinutes <= day.model.profile.sessionMinutes + 1,
                "day ${day.day} ran to ${session.estimatedMinutes} minutes",
            )
        }
    }

    private fun assertPainRespected(day: SimulatedDay, session: PlannedSession) {
        session.plans.forEach { plan ->
            val advice = plan.exercise.pattern.loads.mapNotNull { day.model.pain[it] }
            assertTrue(PainAdvice.SEE_PROFESSIONAL !in advice, "trained an area that needs a professional")
            assertTrue(advice.isEmpty() || !plan.isHard, "hard work on a sore area on day ${day.day}")
        }
    }
}
