package app.levitx.core.skills

import app.levitx.core.load.LoadProfile

/*
 * Short ways to write gates in the path files. A gate's id is its path's id,
 * a dot, then a short key, so every gate id stays unique across the catalogue.
 */

/** A gate passed by [target] clean reps in one set. */
internal fun reps(id: String, title: String, target: Int, trains: String, load: LoadProfile? = null): Gate =
    Gate(id, title, GateMeasure.REPS, target, trains, load)

/** A gate passed by holding for [seconds] in one attempt. */
internal fun hold(id: String, title: String, seconds: Int, trains: String): Gate =
    Gate(id, title, GateMeasure.HOLD_SECONDS, seconds, trains)

/** What the last gate of every path trains. */
internal const val THE_GOAL: String = "The goal."
