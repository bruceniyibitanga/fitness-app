import '../validation.dart';
import 'load_modifier.dart';
import 'load_profile.dart';

/// Input limits for [calculateEffectiveLoad].
///
/// Values outside these are almost certainly typos (a bodyweight of 7 kg,
/// say), so the calculation refuses them instead of producing nonsense.
abstract final class LoadLimits {
  /// Lowest bodyweight accepted, in kilograms.
  static const double minBodyweightKg = 20;

  /// Highest bodyweight accepted, in kilograms.
  static const double maxBodyweightKg = 350;

  /// Most weight a single modifier may add or remove, in kilograms.
  static const double maxModifierKg = 200;
}

/// The load of one rep, with the parts that make it up.
///
/// Keeping the parts lets the app explain a number ("47.4 kg: 64% of your
/// 74 kg") instead of showing a figure nobody can check.
final class EffectiveLoad {
  /// Creates a load breakdown.
  const EffectiveLoad({
    required this.fromBodyweightKg,
    required this.fromAddedWeightKg,
    required this.removedByAssistanceKg,
    required this.isEstimate,
  });

  /// Kilograms of bodyweight moved.
  final double fromBodyweightKg;

  /// Kilograms moved from vests, belts and other added weight.
  final double fromAddedWeightKg;

  /// Kilograms taken off by band assistance.
  final double removedByAssistanceKg;

  /// True when any part rests on an estimated share.
  final bool isEstimate;

  /// Total load per rep in kilograms. Never below zero.
  double get totalKg {
    final total = fromBodyweightKg + fromAddedWeightKg - removedByAssistanceKg;
    return total < 0 ? 0 : total;
  }
}

/// Works out how many kilograms one rep of an exercise moves.
///
/// Added weight is counted at the same [LoadProfile.bodyweightShare] as the
/// body. That is a fair estimate for a vest worn on the torso, and exact for
/// hanging exercises like pull-ups where the share is 1. Band assistance is
/// subtracted in full because it is calibrated as kilograms of help.
///
/// Throws [ArgumentError] if any input is outside [LoadLimits] or the profile
/// share is invalid. The UI validates first, so a throw here is a bug.
EffectiveLoad calculateEffectiveLoad({
  required double bodyweightKg,
  required LoadProfile profile,
  Iterable<LoadModifier> modifiers = const [],
}) {
  checkInRange(
    bodyweightKg,
    LoadLimits.minBodyweightKg,
    LoadLimits.maxBodyweightKg,
    'bodyweightKg',
  );
  if (!profile.isValid) {
    throw ArgumentError.value(profile.id, 'profile', 'share must be in (0, 1]');
  }

  var addedKg = 0.0;
  var assistanceKg = 0.0;
  for (final modifier in modifiers) {
    checkInRange(
      modifier.kilograms,
      0,
      LoadLimits.maxModifierKg,
      'modifier.kilograms',
    );
    switch (modifier) {
      case AddedWeight(:final kilograms):
        addedKg += kilograms;
      case BandAssistance(:final kilograms):
        assistanceKg += kilograms;
    }
  }

  final share = profile.bodyweightShare;
  return EffectiveLoad(
    fromBodyweightKg: bodyweightKg * share,
    fromAddedWeightKg: addedKg * share,
    removedByAssistanceKg: assistanceKg,
    isEstimate: profile.evidence == LoadEvidence.estimated,
  );
}
