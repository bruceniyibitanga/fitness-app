package app.levitx.sync

import app.levitx.core.skills.SkillPath
import app.levitx.data.equipment.Band

/**
 * Everything the watch needs from the phone to plan and run a session.
 *
 * It is deliberately small. Bodyweight, pain notes and anything else the watch
 * can work without stay on the phone (see docs/app/SECURITY.md).
 *
 * @property path The active goal.
 * @property bests The person's best per gate id.
 * @property bands The person's bands, for band-assisted sets.
 */
internal data class PhoneState(val path: SkillPath, val bests: Map<String, Int>, val bands: List<Band>)
