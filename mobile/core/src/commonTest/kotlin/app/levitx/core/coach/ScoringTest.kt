package app.levitx.core.coach

import app.levitx.core.skills.SkillCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScoringTest {
    private val coach = Coach()
    private val pushUp = exercise("horizontal_push.push_up")
    private val squat = exercise("squat.bodyweight")
    private val bridge = exercise("hinge.glute_bridge")
    private val gate = assertNotNull(SkillTraining.exerciseFor(SkillCatalogue.firstPullUp.gates.first()))
    private val skillWork = hardPlan(gate, 10).copy(gateId = gate.id)
    private val fresh = contextOn(0, newcomer())

    @Test
    fun skillWorkScoresHighestTowardTheGoal() {
        val withSkill = GoalTerm.score(sessionOf(skillWork, hardPlan(squat, 12)), fresh)
        val without = GoalTerm.score(sessionOf(hardPlan(pushUp, 8), hardPlan(squat, 12)), fresh)
        assertTrue(withSkill > without)
        assertEquals(0.0, without)
    }

    @Test
    fun lightSkillWorkDoesNotCountTowardTheGoal() {
        assertEquals(0.0, GoalTerm.score(sessionOf(skillWork).lightened(), fresh))
    }

    @Test
    fun withNoGoalNothingScoresTowardOne() {
        val general = contextOn(0, newcomer(CoachProfile(goalPathId = null, equipment = fullKit)))
        assertEquals(0.0, GoalTerm.score(sessionOf(hardPlan(pushUp, 8)), general))
    }

    @Test
    fun volumeFavoursThePatternThatIsBehind() {
        // Squats done three times this week, hip work not at all.
        val model = (0..2).fold(newcomer()) { model, index ->
            coach.record(model, finished(index * 2, done(hardPlan(squat, 12), 12)))
        }
        val context = contextOn(6, model)
        val moreSquats = VolumeTerm.score(sessionOf(hardPlan(squat, 12)), context)
        val hipWork = VolumeTerm.score(sessionOf(hardPlan(bridge, 12)), context)
        assertTrue(hipWork > moreSquats)
        assertEquals(0.0, moreSquats)
    }

    @Test
    fun recoveryFavoursWhatHasRestedLongest() {
        val model = coach.record(newcomer(), finished(0, done(hardPlan(squat, 12), 12)))
        val context = contextOn(2, model)
        val rested = RecoveryTerm.score(sessionOf(hardPlan(pushUp, 8)), context)
        val recent = RecoveryTerm.score(sessionOf(hardPlan(squat, 12)), context)
        assertEquals(1.0, rested)
        assertEquals(48.0 / CoachTuning.FULL_RECOVERY_HOURS, recent, absoluteTolerance = 0.02)
        assertEquals(1.0, RecoveryTerm.score(sessionOf(hardPlan(squat, 12)).lightened(), context))
    }

    @Test
    fun timeFavoursTheSessionThatUsesWhatThePersonHas() {
        val short = sessionOf(hardPlan(pushUp, 8))
        val full = sessionOf(skillWork, hardPlan(pushUp, 8), hardPlan(squat, 12), hardPlan(bridge, 12))
        assertTrue(TimeTerm.score(full, fresh) > TimeTerm.score(short, fresh))
    }

    @Test
    fun preferenceDropsForExercisesThePersonKeepsSkipping() {
        val skipped = newcomer().copy(skips = mapOf(pushUp.id to CoachTuning.SKIPS_BEFORE_DISLIKED))
        val session = sessionOf(hardPlan(pushUp, 8), hardPlan(squat, 12))
        assertEquals(1.0, PreferenceTerm.score(session, fresh))
        assertEquals(0.5, PreferenceTerm.score(session, contextOn(0, skipped)))
    }

    @Test
    fun noveltyFavoursADifferentMixFromLastTime() {
        val model = coach.record(newcomer(), finished(0, done(hardPlan(squat, 12), 12)))
        val context = contextOn(2, model)
        assertEquals(0.0, NoveltyTerm.score(sessionOf(hardPlan(squat, 12)), context))
        assertEquals(1.0, NoveltyTerm.score(sessionOf(hardPlan(pushUp, 8)), context))
        assertEquals(0.0, NoveltyTerm.score(sessionOf(hardPlan(pushUp, 8)), fresh), "no last session, no novelty")
    }

    @Test
    fun weightsAddUpToOneAndNoveltyIsTheSmallest() {
        val weights = ScoringTerms.all.associate { it.term to it.weight }
        assertEquals(1.0, weights.values.sum(), absoluteTolerance = 1e-9)
        assertEquals(Term.NOVELTY, weights.minBy { it.value }.key)
        assertEquals(Term.entries.toSet(), weights.keys)
    }

    @Test
    fun theSameSessionAlwaysScoresTheSame() {
        val session = sessionOf(skillWork, hardPlan(squat, 12))
        assertEquals(SessionScorer().score(session, fresh), SessionScorer().score(session, fresh))
    }
}
