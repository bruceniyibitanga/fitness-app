import 'package:test/test.dart';
import 'package:training_core/training_core.dart';

void main() {
  group('calculateEffectiveLoad', () {
    test('a regular push-up moves 64% of bodyweight', () {
      final load = calculateEffectiveLoad(
        bodyweightKg: 74,
        profile: LoadCatalogue.pushUp,
      );

      expect(load.totalKg, closeTo(47.36, 0.001));
      expect(load.isEstimate, isFalse);
    });

    test('added weight is counted at the same share as bodyweight', () {
      final load = calculateEffectiveLoad(
        bodyweightKg: 74,
        profile: LoadCatalogue.pushUp,
        modifiers: const [AddedWeight(10)],
      );

      expect(load.fromAddedWeightKg, closeTo(6.4, 0.001));
      expect(load.totalKg, closeTo(53.76, 0.001));
    });

    test('a weighted pull-up adds the full belt weight', () {
      final load = calculateEffectiveLoad(
        bodyweightKg: 74,
        profile: LoadCatalogue.pullUp,
        modifiers: const [AddedWeight(5)],
      );

      expect(load.totalKg, closeTo(79, 0.001));
      expect(load.isEstimate, isTrue);
    });

    test('band assistance is subtracted in full', () {
      final load = calculateEffectiveLoad(
        bodyweightKg: 80,
        profile: LoadCatalogue.pullUp,
        modifiers: const [BandAssistance(20)],
      );

      expect(load.totalKg, closeTo(60, 0.001));
    });

    test('total never goes below zero', () {
      final load = calculateEffectiveLoad(
        bodyweightKg: 40,
        profile: LoadCatalogue.pushUpHandsOn61cmBox,
        modifiers: const [BandAssistance(100)],
      );

      expect(load.totalKg, 0);
    });

    test('push-up variations get heavier in catalogue order', () {
      final totals = [
        for (final profile in LoadCatalogue.pushUps)
          calculateEffectiveLoad(bodyweightKg: 80, profile: profile).totalKg,
      ];

      for (var i = 1; i < totals.length; i++) {
        expect(totals[i], greaterThan(totals[i - 1]));
      }
    });

    test('rejects a bodyweight outside the limits', () {
      expect(
        () => calculateEffectiveLoad(
          bodyweightKg: 7,
          profile: LoadCatalogue.pushUp,
        ),
        throwsArgumentError,
      );
      expect(
        () => calculateEffectiveLoad(
          bodyweightKg: double.nan,
          profile: LoadCatalogue.pushUp,
        ),
        throwsArgumentError,
      );
    });

    test('rejects a negative modifier', () {
      expect(
        () => calculateEffectiveLoad(
          bodyweightKg: 74,
          profile: LoadCatalogue.pushUp,
          modifiers: const [AddedWeight(-5)],
        ),
        throwsArgumentError,
      );
    });
  });

  group('LoadCatalogue', () {
    test('every profile has a valid share and a unique id', () {
      final ids = <String>{};
      for (final profile in LoadCatalogue.all) {
        expect(profile.isValid, isTrue, reason: profile.id);
        expect(ids.add(profile.id), isTrue, reason: 'duplicate ${profile.id}');
      }
    });

    test('measured profiles cite a source', () {
      for (final profile in LoadCatalogue.all) {
        if (profile.evidence == LoadEvidence.measured) {
          expect(profile.source, isNotNull, reason: profile.id);
        }
      }
    });

    test('byId finds a profile and returns null for unknown ids', () {
      expect(LoadCatalogue.byId('push_up'), same(LoadCatalogue.pushUp));
      expect(LoadCatalogue.byId('nope'), isNull);
    });
  });
}
