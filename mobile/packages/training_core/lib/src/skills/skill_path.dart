/// What a gate measures.
enum GateMeasure {
  /// Clean reps in one set.
  reps,

  /// Seconds held in one attempt.
  holdSeconds,
}

/// One step on the way to a skill, with a clear pass mark.
final class Gate {
  /// Creates a gate. [target] must be at least 1.
  const Gate({
    required this.id,
    required this.title,
    required this.measure,
    required this.target,
    required this.trains,
  });

  /// Stable identifier used in storage and sync.
  final String id;

  /// Short name, for example "10 pull-ups".
  final String title;

  /// Whether [target] counts reps or seconds.
  final GateMeasure measure;

  /// Reps or seconds needed to pass.
  final int target;

  /// One line on what the gate builds, shown when the user taps it.
  final String trains;
}

/// Where someone is on a [SkillPath].
final class PathProgress {
  /// Creates progress. [currentIndex] is null once every gate is passed.
  const PathProgress({
    required this.currentIndex,
    required this.gatesRemaining,
    required this.currentFraction,
  });

  /// Index of the first gate not yet passed, or null if the path is done.
  final int? currentIndex;

  /// Gates left to pass, including the current one.
  final int gatesRemaining;

  /// Progress toward the current gate, from 0 to 1. 1 when complete.
  final double currentFraction;

  /// True once every gate is passed.
  bool get isComplete => currentIndex == null;
}

/// An ordered list of gates leading to a skill.
///
/// Gates are passed in order: a later gate never counts as passed while an
/// earlier one is still open, because each gate builds what the next needs.
final class SkillPath {
  /// Creates a path.
  const SkillPath({required this.id, required this.name, required this.gates});

  /// Stable identifier used in storage and sync.
  final String id;

  /// Skill name, for example "Muscle-up".
  final String name;

  /// Gates from first to last. The last gate is the skill itself.
  final List<Gate> gates;

  /// Works out progress from the person's best result per gate id.
  ///
  /// Missing ids count as zero. Negative bests are treated as zero.
  PathProgress progressFor(Map<String, int> bestByGateId) {
    for (var i = 0; i < gates.length; i++) {
      final gate = gates[i];
      final best = bestByGateId[gate.id] ?? 0;
      if (best < gate.target) {
        final clamped = best < 0 ? 0 : best;
        return PathProgress(
          currentIndex: i,
          gatesRemaining: gates.length - i,
          currentFraction: clamped / gate.target,
        );
      }
    }
    return const PathProgress(
      currentIndex: null,
      gatesRemaining: 0,
      currentFraction: 1,
    );
  }
}
