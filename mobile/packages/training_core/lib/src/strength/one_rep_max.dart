import '../validation.dart';

/// Most reps (performed plus reps in reserve) the estimate is trusted for.
///
/// Rep-based prediction equations were validated against sets of 10 reps or
/// fewer (LeSuer et al., 1997), so longer sets return null rather than a
/// number that looks precise but isn't.
const int maxRepsForOneRepMaxEstimate = 10;

/// Estimates a one-rep max with the Epley formula: load × (1 + reps / 30).
///
/// [repsInReserve] is how many more reps the person could have done. A set of
/// 5 with 2 in reserve is treated like a set of 7 to failure, which is how
/// reps-in-reserve scales are normally used.
///
/// Returns [loadKg] unchanged for a single rep to failure, and null when the
/// total is above [maxRepsForOneRepMaxEstimate]. Throws [ArgumentError] for
/// negative or non-finite input.
double? estimateOneRepMax({
  required double loadKg,
  required int reps,
  int repsInReserve = 0,
}) {
  checkInRange(loadKg, 0, 1000, 'loadKg');
  checkInRange(reps, 1, 1000, 'reps');
  checkInRange(repsInReserve, 0, 20, 'repsInReserve');

  final repsToFailure = reps + repsInReserve;
  if (repsToFailure > maxRepsForOneRepMaxEstimate) return null;
  if (repsToFailure == 1) return loadKg;
  return loadKg * (1 + repsToFailure / 30);
}
