package app.levitx.core.coach

import app.levitx.core.load.LoadCarrier

/*
 * Which exercises take added weight: the basic strength movements, where the
 * coach adds weight once the person beats the top of the range, before moving
 * them to a harder variation. Pistols, shrimps, archers, one-arm work, ring
 * work, Nordic curls, negatives, holds and anything still being learned never
 * do: balance and control are what they train, and weight would hide that.
 */

/** Hanging or supported on bars: a vest or a dip belt. */
internal fun Exercise.hanging(): Exercise = copy(carriers = setOf(LoadCarrier.VEST, LoadCarrier.BELT))

/** On the floor or standing: a vest. */
internal fun Exercise.wearing(): Exercise = copy(carriers = setOf(LoadCarrier.VEST))

/** Bridges and hip thrusts: plates resting on the hips. */
internal fun Exercise.platesOnHips(): Exercise = copy(carriers = setOf(LoadCarrier.PLATES))
