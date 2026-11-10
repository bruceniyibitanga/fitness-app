/// Throws [ArgumentError] unless [value] is a finite number in [min]..[max].
///
/// Shared by every calculation so bad input fails the same way everywhere.
void checkInRange(num value, num min, num max, String name) {
  if (value.isNaN || value.isInfinite || value < min || value > max) {
    throw ArgumentError.value(value, name, 'must be between $min and $max');
  }
}
