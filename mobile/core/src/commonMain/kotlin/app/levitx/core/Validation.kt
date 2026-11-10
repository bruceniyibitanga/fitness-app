package app.levitx.core

/*
 * Input checks shared by every calculation, so bad input fails the same way
 * everywhere.
 *
 * The messages name the input and the allowed range but never the value
 * itself. Inputs are often health data (bodyweight, pain ratings), and
 * exception messages can end up in crash reports.
 */

/** Throws [IllegalArgumentException] unless [value] is finite and in [min]..[max]. */
internal fun requireInRange(value: Double, min: Double, max: Double, name: String) {
    require(value.isFinite() && value in min..max) { "$name must be between $min and $max" }
}

/** Throws [IllegalArgumentException] unless [value] is in [min]..[max]. */
internal fun requireInRange(value: Int, min: Int, max: Int, name: String) {
    require(value in min..max) { "$name must be between $min and $max" }
}
