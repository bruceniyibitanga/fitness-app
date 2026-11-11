package app.levitx.phone.body

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.BodyweightTrend
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.ExerciseCatalogue
import app.levitx.core.coach.WeighIn
import app.levitx.core.coach.bodyweightTrend
import app.levitx.core.coach.currentExercise
import app.levitx.core.load.LoadEvidence
import app.levitx.core.load.calculateEffectiveLoad
import app.levitx.data.equipment.Band

/** What the Body tab shows. */
sealed interface BodyUiState {
    /** Loading. */
    data object Loading : BodyUiState

    /** The data couldn't be read on this device. */
    data object Unavailable : BodyUiState

    /**
     * Bodyweight, what it means on the bar, and the kit the plan can use.
     *
     * @property nowEpochMillis When this was worked out.
     * @property weighIns Recent readings, oldest first, for the chart.
     * @property trend Where bodyweight is heading, or null before the first reading.
     * @property onTheBar Load per rep of each exercise trained now that has a load figure.
     * @property equipment Kit the plan can use.
     * @property bands Calibrated bands, oldest first.
     */
    data class Ready(
        val nowEpochMillis: Long,
        val weighIns: List<WeighIn>,
        val trend: BodyweightTrend?,
        val onTheBar: List<BarLoad>,
        val equipment: Set<Equipment>,
        val bands: List<Band>,
    ) : BodyUiState
}

/**
 * What one exercise the person trains now puts on the bar.
 *
 * @property exerciseId The exercise, for opening its detail.
 * @property name Its name.
 * @property bodyweightShare Share of bodyweight each rep moves, 0 to 1.
 * @property measured True when the share was measured, false for a reasoned estimate.
 * @property kgPerRep Kilograms each rep moves at today's bodyweight.
 * @property kgPerWeek How [kgPerRep] is changing a week with bodyweight, or
 *   null until the trend can be trusted.
 */
data class BarLoad(
    val exerciseId: String,
    val name: String,
    val bodyweightShare: Double,
    val measured: Boolean,
    val kgPerRep: Double,
    val kgPerWeek: Double?,
)

/**
 * Works out the Body tab from the coach's [model] as of [nowEpochMillis].
 * Load per rep uses the trend's weight, the same figure the coach plans with.
 */
internal fun bodyState(model: AthleteModel, bands: List<Band>, nowEpochMillis: Long): BodyUiState.Ready {
    val trend = bodyweightTrend(model.weighIns, nowEpochMillis)
    return BodyUiState.Ready(
        nowEpochMillis = nowEpochMillis,
        weighIns = model.weighIns.filter { it.atEpochMillis <= nowEpochMillis },
        trend = trend,
        onTheBar = trend?.let { onTheBar(model, it) }.orEmpty(),
        equipment = model.profile.equipment,
        bands = bands,
    )
}

private fun onTheBar(model: AthleteModel, trend: BodyweightTrend): List<BarLoad> = ExerciseCatalogue.ladders
    .mapNotNull { ladder -> model.currentExercise(ladder.pattern) }
    .distinctBy { it.id }
    .mapNotNull { exercise ->
        exercise.loadProfile?.let { profile ->
            BarLoad(
                exerciseId = exercise.id,
                name = exercise.name,
                bodyweightShare = profile.bodyweightShare,
                measured = profile.evidence == LoadEvidence.MEASURED,
                kgPerRep = calculateEffectiveLoad(trend.currentKg, profile).totalKg,
                kgPerWeek = trend.weeklyChange?.let { it * trend.currentKg * profile.bodyweightShare },
            )
        }
    }
