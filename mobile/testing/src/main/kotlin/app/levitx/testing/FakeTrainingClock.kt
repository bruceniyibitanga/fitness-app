package app.levitx.testing

import app.levitx.data.time.TrainingClock

/**
 * A [TrainingClock] tests move by hand.
 *
 * @property elapsed The elapsed (since boot) clock, in milliseconds.
 * @property now The wall clock, in UTC milliseconds.
 */
class FakeTrainingClock(var elapsed: Long = 10_000, var now: Long = 1_700_000_000_000) : TrainingClock {
    override fun elapsedRealtimeMillis(): Long = elapsed

    override fun currentTimeMillis(): Long = now
}
