/// Where a bodyweight share figure comes from.
///
/// The UI shows estimated figures with an "EST" label so people can tell
/// measured numbers from reasoned ones.
enum LoadEvidence {
  /// Measured directly, for example on a force plate.
  measured,

  /// A reasoned estimate that has not been measured for this exercise.
  estimated,
}

/// How much of a person's bodyweight an exercise variation makes them move.
///
/// Calisthenics progresses by changing the exercise rather than the weight,
/// so every variation gets a share. A regular push-up moves about 64% of
/// bodyweight, so its [bodyweightShare] is `0.64`.
final class LoadProfile {
  /// Creates a profile. [bodyweightShare] must be greater than 0 and at most 1.
  const LoadProfile({
    required this.id,
    required this.name,
    required this.bodyweightShare,
    required this.evidence,
    this.source,
  });

  /// Stable identifier used in storage and sync. Never reuse or rename one.
  final String id;

  /// Human-readable name shown in the app.
  final String name;

  /// Fraction of bodyweight moved per rep, between 0 (exclusive) and 1.
  final double bodyweightShare;

  /// Whether [bodyweightShare] was measured or estimated.
  final LoadEvidence evidence;

  /// Citation for a measured figure. Null for estimates.
  final String? source;

  /// True when [bodyweightShare] is inside the allowed range.
  bool get isValid => bodyweightShare > 0 && bodyweightShare <= 1;
}
