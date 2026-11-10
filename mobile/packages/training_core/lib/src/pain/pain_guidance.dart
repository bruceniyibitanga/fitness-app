import '../validation.dart';

/// Warning signs that mean training advice should stop and a professional
/// should take over.
///
/// This list is a starting point and must be reviewed by a physiotherapist
/// before release.
enum RedFlag {
  /// Pain that wakes the person at night or is there at rest.
  nightOrRestPain,

  /// Numbness, tingling or pins and needles.
  numbnessOrTingling,

  /// Visible swelling, bruising or a joint that looks wrong.
  swellingOrDeformity,

  /// Pain that started with a fall, pop or impact.
  suddenInjury,

  /// Weakness that is getting worse.
  worseningWeakness,

  /// Fever or unexplained weight loss alongside the pain.
  feverOrWeightLoss,
}

/// What the app suggests after a pain check-in.
enum PainAdvice {
  /// Pain is inside the guide. Train as planned.
  continueAsPlanned,

  /// Ease off: swap to a lighter variation or reduce sets.
  easeOff,

  /// Stop giving training advice for this area and see a professional.
  seeProfessional,
}

/// Why the advice was given, so the UI can explain it in plain words.
enum PainReason {
  /// One or more [RedFlag]s were reported.
  redFlag,

  /// Pain during exercise went above the guide's limit.
  tooHighDuringExercise,

  /// Pain the next morning was worse than the morning before.
  worseNextMorning,

  /// Everything was inside the guide.
  withinGuide,
}

/// A pain check-in for one body area. Ratings are 0 (none) to 10 (worst).
final class PainCheckIn {
  /// Creates a check-in. Morning ratings are optional because they come later.
  const PainCheckIn({
    required this.duringExercise,
    this.morningBefore,
    this.morningAfter,
    this.redFlags = const {},
  });

  /// Highest pain during the session.
  final int duringExercise;

  /// Pain or stiffness on the morning of the session, before training.
  final int? morningBefore;

  /// Pain or stiffness on the morning after the session.
  final int? morningAfter;

  /// Warning signs the person reported.
  final Set<RedFlag> redFlags;
}

/// The result of [assessPain].
final class PainAssessment {
  /// Creates an assessment.
  const PainAssessment(this.advice, this.reason);

  /// What to do next.
  final PainAdvice advice;

  /// Why.
  final PainReason reason;
}

/// Highest pain during exercise the guide accepts.
const int maxAcceptablePainDuringExercise = 5;

/// Turns a check-in into advice, following the pain-monitoring model
/// (Silbernagel et al., 2007): pain during exercise may reach 5 out of 10,
/// should settle by the next morning, and should not build up over time.
///
/// This is a training guide, not a diagnosis. Red flags always win.
/// Throws [ArgumentError] if a rating is outside 0 to 10.
PainAssessment assessPain(PainCheckIn checkIn) {
  checkInRange(checkIn.duringExercise, 0, 10, 'duringExercise');
  final before = checkIn.morningBefore;
  final after = checkIn.morningAfter;
  if (before != null) checkInRange(before, 0, 10, 'morningBefore');
  if (after != null) checkInRange(after, 0, 10, 'morningAfter');

  if (checkIn.redFlags.isNotEmpty) {
    return const PainAssessment(PainAdvice.seeProfessional, PainReason.redFlag);
  }
  if (checkIn.duringExercise > maxAcceptablePainDuringExercise) {
    return const PainAssessment(
      PainAdvice.easeOff,
      PainReason.tooHighDuringExercise,
    );
  }
  if (before != null && after != null && after > before) {
    return const PainAssessment(
      PainAdvice.easeOff,
      PainReason.worseNextMorning,
    );
  }
  return const PainAssessment(
    PainAdvice.continueAsPlanned,
    PainReason.withinGuide,
  );
}
