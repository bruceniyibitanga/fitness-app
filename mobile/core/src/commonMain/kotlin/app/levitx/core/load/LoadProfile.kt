package app.levitx.core.load

/**
 * Where a bodyweight share figure comes from.
 *
 * The apps label estimated figures "EST" so people can tell measured numbers
 * from reasoned ones.
 */
public enum class LoadEvidence {
    /** Measured directly, for example on a force plate. */
    MEASURED,

    /** A reasoned estimate that has not been measured for this exercise. */
    ESTIMATED,
}

/**
 * How much of a person's bodyweight an exercise variation makes them move.
 *
 * Calisthenics progresses by changing the exercise rather than the weight, so
 * every variation gets a share. A regular push-up moves about 64% of
 * bodyweight, so its [bodyweightShare] is `0.64`.
 *
 * @property id Stable identifier used in storage and sync. Never reuse or rename one.
 * @property name Human-readable name shown in the apps.
 * @property bodyweightShare Fraction of bodyweight moved per rep, above 0 and at most 1.
 * @property evidence Whether [bodyweightShare] was measured or estimated.
 * @property source Citation for a measured figure. Null for estimates.
 * @throws IllegalArgumentException if [bodyweightShare] is outside (0, 1], or
 *   a measured profile has no source.
 */
public data class LoadProfile(
    val id: String,
    val name: String,
    val bodyweightShare: Double,
    val evidence: LoadEvidence,
    val source: String? = null,
) {
    init {
        require(bodyweightShare > 0.0 && bodyweightShare <= 1.0) { "bodyweightShare must be in (0, 1]" }
        require(evidence != LoadEvidence.MEASURED || source != null) { "a measured profile needs a source" }
    }
}
