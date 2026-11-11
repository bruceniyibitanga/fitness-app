package app.levitx.data.coach

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.BodyArea
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Effort
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.ExerciseMark
import app.levitx.core.coach.ExerciseState
import app.levitx.core.coach.MovementPattern
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.ProgressNote
import app.levitx.core.coach.SessionRecord
import app.levitx.core.coach.SuggestionChoice
import app.levitx.core.coach.SuggestionOutcome
import app.levitx.core.coach.TrainingBlock
import app.levitx.core.pain.PainAdvice
import java.time.DayOfWeek
import kotlinx.serialization.Serializable

/**
 * Format of the coach's stored document. Bump it when a change can't be read
 * by the code as it was before, and add the step from the old format to
 * [CoachCodec]. Adding a field with a default doesn't need a bump.
 */
internal const val COACH_DOCUMENT_VERSION = 1

/**
 * The coach's stored document, version [COACH_DOCUMENT_VERSION].
 *
 * It mirrors `AthleteModel` field by field, as its own type, so the stored
 * format is a contract that only changes on purpose. Renaming something in
 * `:core` can't silently change what is on disk. Bests and bodyweight aren't
 * here: they live in their own tables (see [CoachState]).
 */
@Serializable
internal data class CoachDocument(
    val v: Int,
    val profile: StoredProfile,
    val trainingDays: List<Int> = emptyList(),
    val rungs: Map<String, Int> = emptyMap(),
    val exercises: Map<String, StoredExerciseState> = emptyMap(),
    val gateBests: Map<String, Int> = emptyMap(),
    val history: List<StoredSessionRecord> = emptyList(),
    val pain: Map<String, String> = emptyMap(),
    val block: StoredBlock = StoredBlock(),
    val suggestionOutcomes: List<StoredOutcome> = emptyList(),
    val skips: Map<String, Int> = emptyMap(),
    val weightNoticeShown: Boolean = false,
    val lastEasedAt: Long? = null,
)

/** A stored `CoachProfile`. */
@Serializable
internal data class StoredProfile(
    val goalPathId: String?,
    val style: String,
    val sessionsPerWeek: Int,
    val sessionMinutes: Int,
    val equipment: List<String> = emptyList(),
)

/** A stored `ExerciseState`. */
@Serializable
internal data class StoredExerciseState(
    val target: Int,
    val best: Int = 0,
    val timesTrained: Int = 0,
    val marks: List<StoredMark> = emptyList(),
    val note: String? = null,
)

/** A stored `ExerciseMark`. */
@Serializable
internal data class StoredMark(val at: Long, val top: Int, val effort: String? = null, val hitTarget: Boolean)

/** A stored `SessionRecord`. */
@Serializable
internal data class StoredSessionRecord(
    val finishedAt: Long,
    val directSets: Map<String, Int> = emptyMap(),
    val hardSets: Map<String, Double> = emptyMap(),
    val exerciseIds: List<String> = emptyList(),
    val minutes: Int = 0,
)

/** A stored `TrainingBlock`. */
@Serializable
internal data class StoredBlock(val index: Int = 0, val sessionsDone: Int = 0, val lighterSessionsLeft: Int = 0)

/** A stored `SuggestionOutcome`. */
@Serializable
internal data class StoredOutcome(val choice: String, val wentWell: Boolean)

/** The document for [model] and [trainingDays]. Bodyweight readings are left out on purpose. */
internal fun coachDocumentOf(model: AthleteModel, trainingDays: Set<DayOfWeek>): CoachDocument = CoachDocument(
    v = COACH_DOCUMENT_VERSION,
    profile = model.profile.let {
        StoredProfile(
            it.goalPathId,
            it.style.name,
            it.sessionsPerWeek,
            it.sessionMinutes,
            it.equipment.map { e ->
                e.name
            },
        )
    },
    trainingDays = trainingDays.map { it.value }.sorted(),
    rungs = model.rungs.withNameKeys(),
    exercises = model.exercises.mapValues { (_, state) ->
        StoredExerciseState(
            target = state.target,
            best = state.best,
            timesTrained = state.timesTrained,
            marks = state.marks.map { StoredMark(it.atEpochMillis, it.top, it.effort?.name, it.hitTarget) },
            note = state.note?.name,
        )
    },
    gateBests = model.gateBests,
    history = model.history.map {
        StoredSessionRecord(
            finishedAt = it.finishedAtEpochMillis,
            directSets = it.directSets.withNameKeys(),
            hardSets = it.hardSets.withNameKeys(),
            exerciseIds = it.exerciseIds,
            minutes = it.minutes,
        )
    },
    pain = model.pain.mapKeys { it.key.name }.mapValues { it.value.name },
    block = model.block.let { StoredBlock(it.index, it.sessionsDone, it.lighterSessionsLeft) },
    suggestionOutcomes = model.suggestionOutcomes.map { StoredOutcome(it.choice.name, it.wentWell) },
    skips = model.skips,
    weightNoticeShown = model.weightNoticeShown,
    lastEasedAt = model.lastEasedAtEpochMillis,
)

/**
 * The model this document describes, without bodyweight readings.
 *
 * @throws IllegalArgumentException when a value breaks the model's own rules,
 *   for example a goal that no longer exists.
 */
internal fun CoachDocument.toModel(): AthleteModel = AthleteModel(
    profile = CoachProfile(
        goalPathId = profile.goalPathId,
        style = profile.style.toEnumOrNull<PlanStyle>() ?: PlanStyle.ALL_ROUND,
        sessionsPerWeek = profile.sessionsPerWeek,
        sessionMinutes = profile.sessionMinutes,
        equipment = profile.equipment.mapNotNull { it.toEnumOrNull<Equipment>() }.toSet(),
    ),
    rungs = rungs.withEnumKeys<MovementPattern, Int>(),
    exercises = exercises.mapValues { (_, state) ->
        ExerciseState(
            target = state.target,
            best = state.best,
            timesTrained = state.timesTrained,
            marks = state.marks.map { ExerciseMark(it.at, it.top, it.effort?.toEnumOrNull<Effort>(), it.hitTarget) },
            note = state.note?.toEnumOrNull<ProgressNote>(),
        )
    },
    gateBests = gateBests,
    history = history.map {
        SessionRecord(
            finishedAtEpochMillis = it.finishedAt,
            directSets = it.directSets.withEnumKeys<MovementPattern, Int>(),
            hardSets = it.hardSets.withEnumKeys<MovementPattern, Double>(),
            exerciseIds = it.exerciseIds,
            minutes = it.minutes,
        )
    },
    pain = pain.withEnumKeys<BodyArea, String>().mapNotNull { (area, advice) ->
        advice.toEnumOrNull<PainAdvice>()?.let { area to it }
    }.toMap(),
    block = TrainingBlock(block.index, block.sessionsDone, block.lighterSessionsLeft),
    suggestionOutcomes = suggestionOutcomes.mapNotNull { outcome ->
        outcome.choice.toEnumOrNull<SuggestionChoice>()?.let { SuggestionOutcome(it, outcome.wentWell) }
    },
    skips = skips,
    weightNoticeShown = weightNoticeShown,
    lastEasedAtEpochMillis = lastEasedAt,
)

/** The training days in this document. Numbers outside 1 to 7 are dropped. */
internal fun CoachDocument.days(): Set<DayOfWeek> = trainingDays.filter {
    it in 1..DAYS_IN_WEEK
}.map(DayOfWeek::of).toSet()

private const val DAYS_IN_WEEK = 7
