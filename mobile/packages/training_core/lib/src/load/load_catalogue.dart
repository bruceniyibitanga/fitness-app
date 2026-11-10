import 'load_profile.dart';

const _ebben2011 =
    'Ebben et al. (2011), Kinetic analysis of several variations of '
    'push-ups, J Strength Cond Res 25(10):2891-2894';

/// The load profiles the app knows about.
///
/// Push-up figures are force-plate measurements. Pull-ups and dips count the
/// whole body as moved, which is an estimate until we find a measured figure.
/// Add new profiles here rather than hard-coding shares elsewhere.
abstract final class LoadCatalogue {
  /// Push-up with hands on a 61 cm box (easiest incline).
  static const pushUpHandsOn61cmBox = LoadProfile(
    id: 'push_up.hands_61cm',
    name: 'Push-up, hands on 61 cm box',
    bodyweightShare: 0.41,
    evidence: LoadEvidence.measured,
    source: _ebben2011,
  );

  /// Push-up from the knees.
  static const pushUpKnees = LoadProfile(
    id: 'push_up.knees',
    name: 'Knee push-up',
    bodyweightShare: 0.49,
    evidence: LoadEvidence.measured,
    source: _ebben2011,
  );

  /// Push-up with hands on a 30 cm box.
  static const pushUpHandsOn30cmBox = LoadProfile(
    id: 'push_up.hands_30cm',
    name: 'Push-up, hands on 30 cm box',
    bodyweightShare: 0.55,
    evidence: LoadEvidence.measured,
    source: _ebben2011,
  );

  /// Regular push-up on the floor.
  static const pushUp = LoadProfile(
    id: 'push_up',
    name: 'Push-up',
    bodyweightShare: 0.64,
    evidence: LoadEvidence.measured,
    source: _ebben2011,
  );

  /// Push-up with feet on a 30 cm box.
  static const pushUpFeetOn30cmBox = LoadProfile(
    id: 'push_up.feet_30cm',
    name: 'Push-up, feet on 30 cm box',
    bodyweightShare: 0.70,
    evidence: LoadEvidence.measured,
    source: _ebben2011,
  );

  /// Push-up with feet on a 61 cm box (hardest decline measured).
  static const pushUpFeetOn61cmBox = LoadProfile(
    id: 'push_up.feet_61cm',
    name: 'Push-up, feet on 61 cm box',
    bodyweightShare: 0.74,
    evidence: LoadEvidence.measured,
    source: _ebben2011,
  );

  /// Pull-up. Counts the whole body as moved (estimate).
  static const pullUp = LoadProfile(
    id: 'pull_up',
    name: 'Pull-up',
    bodyweightShare: 1,
    evidence: LoadEvidence.estimated,
  );

  /// Parallel bar dip. Counts the whole body as moved (estimate).
  static const dip = LoadProfile(
    id: 'dip',
    name: 'Dip',
    bodyweightShare: 1,
    evidence: LoadEvidence.estimated,
  );

  /// Push-up variations from easiest to hardest.
  static const List<LoadProfile> pushUps = [
    pushUpHandsOn61cmBox,
    pushUpKnees,
    pushUpHandsOn30cmBox,
    pushUp,
    pushUpFeetOn30cmBox,
    pushUpFeetOn61cmBox,
  ];

  /// Every profile in the catalogue.
  static const List<LoadProfile> all = [...pushUps, pullUp, dip];

  /// Finds a profile by its [LoadProfile.id], or returns null.
  static LoadProfile? byId(String id) {
    for (final profile in all) {
      if (profile.id == id) return profile;
    }
    return null;
  }
}
