package app.levitx.sync

import app.levitx.core.coach.PlannedSession
import app.levitx.core.skills.SkillPath
import app.levitx.data.equipment.Band

/**
 * Everything the watch needs from the phone to run a session.
 *
 * It is deliberately small. The coach and everything it knows about the
 * person (bodyweight, pain, training history) stay on the phone; only the
 * session it planned crosses (see docs/app/SECURITY.md).
 *
 * @property path The active goal, or null for general fitness.
 * @property bests The person's best per gate id.
 * @property bands The person's bands, for band-assisted sets.
 * @property next The next session the coach planned, or null when there is nothing safe to train.
 * @property recentWorkoutIds The workouts the coach has recorded most recently,
 *   so the watch can tell [next] already takes in one it finished.
 */
internal data class PhoneState(
    val path: SkillPath?,
    val bests: Map<String, Int>,
    val bands: List<Band>,
    val next: PlannedSession? = null,
    val recentWorkoutIds: List<String> = emptyList(),
)
