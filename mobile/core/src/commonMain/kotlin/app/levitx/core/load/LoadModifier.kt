package app.levitx.core.load

/**
 * Something that changes how much a rep weighs beyond bodyweight alone.
 *
 * The interface is sealed, so every `when` over modifiers must handle every
 * kind. Adding a new kind is a compile error until each place handles it.
 */
public sealed interface LoadModifier {
    /** Size of the modifier in kilograms. Zero or more. */
    public val kilograms: Double
}

/**
 * Weight carried on the body: a vest, a dip belt or ankle weights.
 *
 * @property kilograms How much the weight adds.
 */
public data class AddedWeight(override val kilograms: Double) : LoadModifier

/**
 * Help from a resistance band, in kilograms of assistance.
 *
 * Band tension changes through a rep, so this is the average help the person
 * measured when calibrating the band in the app.
 *
 * @property kilograms How much the band takes off.
 */
public data class BandAssistance(override val kilograms: Double) : LoadModifier
