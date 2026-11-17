package app.levitx.phone.train

import app.levitx.core.coach.Coach
import app.levitx.core.coach.WorkoutFocus
import app.levitx.testing.newcomer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainSearchTest {
    private val model = newcomer()
    private val ready = TrainUiState.Ready(programsFor(model), workoutsFor(model, Coach(), nowEpochMillis = 0L))

    private fun search(query: String) = searchTrain(query, ready) { it.name.lowercase().replace('_', ' ') }

    @Test
    fun aBlankQueryFindsNothing() {
        assertTrue(search("").isEmpty)
        assertTrue(search("  - ").isEmpty)
    }

    @Test
    fun wordsMatchTheStartOfAnyWordInTheName() {
        val pull = search("pull")
        assertTrue(pull.programs.any { it.path.id == "first_pull_up" })
        assertTrue(pull.exercises.any { it.name == "Chest-to-bar pull-up" })
        assertEquals(listOf(WorkoutFocus.PULL), pull.workouts.map { it.focus })
        val lever = search("front lever").programs
        assertTrue(lever.isNotEmpty() && lever.all { "front lever" in it.path.name.lowercase() })
    }

    @Test
    fun shortFragmentsOnlyMatchTheStartOfAWord() {
        assertFalse("ull is inside a word, and short", search("ull").exercises.any { it.name == "Pull-up" })
        assertTrue(
            "but a longer fragment works across the hyphen",
            search("pullup").exercises.any {
                it.name ==
                    "Pull-up"
            },
        )
    }

    @Test
    fun cardioAndMobilityAreFoundToo() {
        assertTrue(search("tabata").intervals.any { it.id == "tabata" })
        assertTrue(search("pigeon").exercises.any { it.id == "mobility.pigeon" })
    }

    @Test
    fun namesThatStartWithTheQueryComeFirst() {
        val names = search("pull").exercises.map { it.name }
        val firstNotStarting = names.indexOfFirst { !it.lowercase().startsWith("pull") }
        assertTrue(names.drop(firstNotStarting.coerceAtLeast(0)).none { it.lowercase().startsWith("pull") })
    }

    @Test
    fun matchingIgnoresCaseAndPunctuation() {
        assertTrue(matches("World's greatest stretch", listOf("worlds")))
        assertTrue(matches("Chest-to-bar pull-up", listOf("chest", "bar")))
        assertFalse(matches("Dead hang", listOf("dead", "lift")))
    }
}
