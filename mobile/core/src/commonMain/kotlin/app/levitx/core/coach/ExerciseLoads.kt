package app.levitx.core.coach

import app.levitx.core.load.LoadCarrier

/*
 * Which exercises take added weight. Only ones the person already does well
 * with bodyweight: the coach adds weight only at the top of a ladder. Pistols,
 * Nordic curls, negatives, holds and anything still being learned never do.
 */

/** Hanging or supported on bars: a vest or a dip belt. */
internal fun Exercise.hanging(): Exercise = copy(carriers = setOf(LoadCarrier.VEST, LoadCarrier.BELT))

/** On the floor or standing: a vest. */
internal fun Exercise.wearing(): Exercise = copy(carriers = setOf(LoadCarrier.VEST))

/** Bridges and hip thrusts: plates resting on the hips. */
internal fun Exercise.platesOnHips(): Exercise = copy(carriers = setOf(LoadCarrier.PLATES))
