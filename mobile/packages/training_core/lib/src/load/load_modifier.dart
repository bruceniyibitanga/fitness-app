/// Something that changes how much a rep weighs beyond bodyweight alone.
///
/// The class is sealed so every place that handles modifiers must handle
/// every kind. Adding a new kind is a compile error until it is handled.
sealed class LoadModifier {
  /// Base constructor for the subclasses.
  const LoadModifier(this.kilograms);

  /// Size of the modifier in kilograms. Always zero or more.
  final double kilograms;
}

/// Weight carried on the body: a vest, a dip belt or ankle weights.
final class AddedWeight extends LoadModifier {
  /// Creates added weight of [kilograms].
  const AddedWeight(super.kilograms);
}

/// Help from a resistance band, in kilograms of assistance.
///
/// Band tension changes through a rep, so this is the average help the user
/// measured when calibrating the band in the app.
final class BandAssistance extends LoadModifier {
  /// Creates band assistance of [kilograms].
  const BandAssistance(super.kilograms);
}
