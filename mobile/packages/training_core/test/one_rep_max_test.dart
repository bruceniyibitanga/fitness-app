import 'package:test/test.dart';
import 'package:training_core/training_core.dart';

void main() {
  group('estimateOneRepMax', () {
    test('uses the Epley formula', () {
      // 100 kg for 5 reps: 100 × (1 + 5/30) = 116.67
      expect(estimateOneRepMax(loadKg: 100, reps: 5), closeTo(116.667, 0.001));
    });

    test('adds reps in reserve to the reps performed', () {
      expect(
        estimateOneRepMax(loadKg: 100, reps: 5, repsInReserve: 2),
        estimateOneRepMax(loadKg: 100, reps: 7),
      );
    });

    test('a single rep to failure is the max', () {
      expect(estimateOneRepMax(loadKg: 90, reps: 1), 90);
    });

    test('returns null above the trusted rep range', () {
      expect(estimateOneRepMax(loadKg: 70, reps: 11), isNull);
      expect(estimateOneRepMax(loadKg: 70, reps: 8, repsInReserve: 3), isNull);
    });

    test('rejects invalid input', () {
      expect(() => estimateOneRepMax(loadKg: 70, reps: 0), throwsArgumentError);
      expect(() => estimateOneRepMax(loadKg: -1, reps: 5), throwsArgumentError);
      expect(
        () => estimateOneRepMax(loadKg: 70, reps: 5, repsInReserve: -1),
        throwsArgumentError,
      );
    });
  });
}
