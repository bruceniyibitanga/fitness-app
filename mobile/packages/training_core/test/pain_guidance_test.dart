import 'package:test/test.dart';
import 'package:training_core/training_core.dart';

void main() {
  group('assessPain', () {
    test('pain up to 5 that settles by morning is within the guide', () {
      final result = assessPain(
        const PainCheckIn(duringExercise: 5, morningBefore: 2, morningAfter: 2),
      );

      expect(result.advice, PainAdvice.continueAsPlanned);
      expect(result.reason, PainReason.withinGuide);
    });

    test('pain above 5 during exercise means ease off', () {
      final result = assessPain(const PainCheckIn(duringExercise: 6));

      expect(result.advice, PainAdvice.easeOff);
      expect(result.reason, PainReason.tooHighDuringExercise);
    });

    test('a worse next morning means ease off', () {
      final result = assessPain(
        const PainCheckIn(duringExercise: 3, morningBefore: 1, morningAfter: 3),
      );

      expect(result.advice, PainAdvice.easeOff);
      expect(result.reason, PainReason.worseNextMorning);
    });

    test('a red flag always means see a professional', () {
      final result = assessPain(
        const PainCheckIn(
          duringExercise: 0,
          redFlags: {RedFlag.numbnessOrTingling},
        ),
      );

      expect(result.advice, PainAdvice.seeProfessional);
      expect(result.reason, PainReason.redFlag);
    });

    test('missing morning ratings do not block advice', () {
      final result = assessPain(const PainCheckIn(duringExercise: 4));

      expect(result.advice, PainAdvice.continueAsPlanned);
    });

    test('rejects ratings outside 0 to 10', () {
      const tooHigh = PainCheckIn(duringExercise: 11);
      const negativeMorning = PainCheckIn(duringExercise: 2, morningAfter: -1);

      expect(() => assessPain(tooHigh), throwsArgumentError);
      expect(() => assessPain(negativeMorning), throwsArgumentError);
    });
  });
}
