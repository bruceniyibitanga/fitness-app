import 'package:test/test.dart';
import 'package:training_core/training_core.dart';

void main() {
  const path = SkillCatalogue.muscleUp;

  group('SkillPath.progressFor', () {
    test('starts at the first gate with no results', () {
      final progress = path.progressFor(const {});

      expect(progress.currentIndex, 0);
      expect(progress.gatesRemaining, path.gates.length);
      expect(progress.currentFraction, 0);
    });

    test('reports progress toward the current gate', () {
      final progress = path.progressFor(const {
        'muscle_up.dead_hang': 30,
        'muscle_up.dips': 10,
        'muscle_up.pull_ups': 8,
      });

      expect(progress.currentIndex, 2);
      expect(progress.gatesRemaining, 4);
      expect(progress.currentFraction, closeTo(0.8, 0.001));
    });

    test('a later gate does not count while an earlier one is open', () {
      final progress = path.progressFor(const {
        'muscle_up.dead_hang': 10,
        'muscle_up.pull_ups': 15,
      });

      expect(progress.currentIndex, 0);
    });

    test('is complete when every gate is passed', () {
      final bests = {for (final gate in path.gates) gate.id: gate.target};

      final progress = path.progressFor(bests);

      expect(progress.isComplete, isTrue);
      expect(progress.gatesRemaining, 0);
      expect(progress.currentFraction, 1);
    });

    test('treats a negative best as zero', () {
      final progress = path.progressFor(const {'muscle_up.dead_hang': -5});

      expect(progress.currentFraction, 0);
    });
  });

  group('SkillCatalogue', () {
    test('gate ids are unique and targets are at least 1', () {
      for (final skill in SkillCatalogue.all) {
        final ids = <String>{};
        for (final gate in skill.gates) {
          expect(ids.add(gate.id), isTrue, reason: 'duplicate ${gate.id}');
          expect(gate.target, greaterThanOrEqualTo(1), reason: gate.id);
        }
      }
    });
  });
}
