package app.levitx.core.coach

import app.levitx.core.skills.SkillCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExerciseCatalogueTest {
    private val squat = ExerciseCatalogue.ladder(MovementPattern.SQUAT)
    private val rows = ExerciseCatalogue.ladder(MovementPattern.HORIZONTAL_PULL)

    @Test
    fun everyPatternHasALadder() {
        MovementPattern.entries.forEach { assertEquals(it, ExerciseCatalogue.ladder(it).pattern) }
    }

    @Test
    fun exerciseIdsAreUnique() {
        val ids = ExerciseCatalogue.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everySkillGateCanBeTrained() {
        SkillCatalogue.all.flatMap { it.gates }.forEach { gate ->
            val exercise = assertNotNull(SkillTraining.exerciseFor(gate), "no training set up for ${gate.id}")
            assertEquals(gate.id, exercise.id)
            assertEquals(gate.measure, exercise.measure)
        }
    }

    @Test
    fun gateIdsNeverCollideWithLadderIds() {
        val gateIds = SkillCatalogue.all.flatMap { it.gates }.map { it.id }.toSet()
        assertTrue(ExerciseCatalogue.all.none { it.id in gateIds })
    }

    @Test
    fun variantsRotateThroughARungInOrder() {
        val rung = squat.rungs.indexOfFirst { it.options.size == 2 }
        val first = squat.exerciseAt(rung, emptySet(), variant = 0)
        val second = squat.exerciseAt(rung, emptySet(), variant = 1)
        assertTrue(first != second)
        assertEquals(first, squat.exerciseAt(rung, emptySet(), variant = 2))
    }

    @Test
    fun missingKitFallsBackToWhatThePersonHas() {
        assertEquals("horizontal_pull.band_row", rows.exerciseAt(0, setOf(Equipment.BAND))?.id)
        assertNull(rows.exerciseAt(1, setOf(Equipment.BAND)))
        assertEquals(0, rows.usableRungAtOrBelow(2, setOf(Equipment.BAND)))
    }

    @Test
    fun aPatternWithNoKitHasNothingToTrain() {
        assertNull(rows.usableRungAtOrBelow(rows.rungs.lastIndex, emptySet()))
    }

    @Test
    fun asksOnlyAboutKitThatUnlocksAPattern() {
        val nothing = ExerciseCatalogue.equipmentWorthAskingAbout(emptySet())
        assertTrue(Equipment.PULL_UP_BAR in nothing && Equipment.RINGS in nothing)
        assertTrue(Equipment.DIP_BARS !in nothing, "dips don't unlock a pattern, so they aren't worth a question")
        assertEquals(emptySet(), ExerciseCatalogue.equipmentWorthAskingAbout(fullKit))
    }

    @Test
    fun aSetCountsInFullForItsPatternAndHalfForWhatItAlsoTrains() {
        val counts = exercise("horizontal_pull.row").setsByPattern(3)
        assertEquals(3.0, counts[MovementPattern.HORIZONTAL_PULL])
        assertEquals(1.5, counts[MovementPattern.VERTICAL_PULL])
    }

    @Test
    fun everySlowNegativeIsTimedAndNothingElseIs() {
        val all = ExerciseCatalogue.ladders.flatMap { ladder -> ladder.rungs.flatMap { it.options } }
        val timed = all.filter { it.lowerSeconds != null }.map { it.id }.toSet()
        assertEquals(
            setOf(
                "vertical_pull.negative",
                "vertical_push.negative_dip",
                "vertical_push.wall_negative",
                "hinge.nordic_negative",
            ),
            timed,
        )
        all.filter { it.id in timed }.forEach { negative ->
            assertEquals(CoachTuning.NEGATIVE_LOWER_SECONDS, negative.lowerSeconds)
        }
    }
}
