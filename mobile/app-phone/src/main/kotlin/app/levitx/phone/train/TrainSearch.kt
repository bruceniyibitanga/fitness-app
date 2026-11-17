package app.levitx.phone.train

import app.levitx.core.coach.Exercise
import app.levitx.core.coach.ExerciseCatalogue
import app.levitx.core.coach.WorkoutFocus
import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalWorkout
import java.util.Locale

/** Shorter fragments only match the start of a word, so "up" doesn't find every "pull-up". */
private const val MIN_INSIDE_MATCH = 4

/**
 * Everything in [ready] and the catalogues that matches [query]. Runs on the
 * phone only: the query is never stored or sent anywhere.
 *
 * Every word of the query has to match the start of a word in the name, so
 * "pull" finds "Chest-to-bar pull-up" and "front lever" finds "Tuck front
 * lever". Longer words also match across hyphens and spaces, so "pullup"
 * finds "Pull-up". Names that start with the query come first.
 *
 * @param focusName What the app calls each single workout, in the phone's language.
 */
fun searchTrain(query: String, ready: TrainUiState.Ready, focusName: (WorkoutFocus) -> String): SearchResults {
    val terms = words(query)
    if (terms.isEmpty()) return SearchResults(emptyList(), emptyList(), emptyList(), emptyList())
    val exercises = (ExerciseCatalogue.all + IntervalCatalogue.moves).distinctBy { it.id }
    return SearchResults(
        programs = ready.programs.filter { matches(it.path.name, terms) }.firstStarting(terms) { it.path.name },
        workouts = ready.workouts.filter { matches(focusName(it.focus), terms) },
        intervals = IntervalCatalogue.all.filter { matches(it.name, terms) }.firstStarting(terms) { it.name },
        exercises = exercises.filter { matches(it.name, terms) }.firstStarting(terms) { it.name },
    )
}

/**
 * What a search on the Train tab found.
 *
 * @property programs Skill programs.
 * @property workouts Single workouts.
 * @property intervals Cardio and mobility workouts.
 * @property exercises Exercises and interval moves, for their how-to.
 */
data class SearchResults(
    val programs: List<ProgramSummary>,
    val workouts: List<WorkoutSummary>,
    val intervals: List<IntervalWorkout>,
    val exercises: List<Exercise>,
) {
    /** True when nothing matched. */
    val isEmpty: Boolean
        get() = programs.isEmpty() && workouts.isEmpty() && intervals.isEmpty() && exercises.isEmpty()
}

/** True when every one of [terms] matches [name]. */
internal fun matches(name: String, terms: List<String>): Boolean {
    val nameWords = words(name)
    val joined = nameWords.joinToString("")
    return terms.all { term ->
        nameWords.any { it.startsWith(term) } || (term.length >= MIN_INSIDE_MATCH && term in joined)
    }
}

/** Lower-case words, split on anything that isn't a letter or a digit. */
private fun words(text: String): List<String> =
    text.lowercase(Locale.ROOT).split(Regex("[^\\p{L}\\p{Nd}]+")).filter { it.isNotEmpty() }

/** Keeps the order, but puts names that start with the query's first word first. */
private fun <T> List<T>.firstStarting(terms: List<String>, name: (T) -> String): List<T> =
    sortedBy { item -> if (words(name(item)).firstOrNull()?.startsWith(terms.first()) == true) 0 else 1 }
