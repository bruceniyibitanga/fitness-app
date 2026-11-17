package app.levitx.phone.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.SkillTraining
import app.levitx.core.skills.SkillLevel
import app.levitx.core.skills.SkillPath
import app.levitx.phone.R

/*
 * How a program is described in short, so the Train cards, the program page
 * and setup all say the same thing: "~8 WK · 5 GATES · BAR".
 */

/** Roughly how long, how many gates and what kit, for a card or a heading. */
@Composable
fun programMeta(path: SkillPath): String = listOf(
    stringResource(R.string.program_weeks, path.weeks),
    pluralStringResource(R.plurals.program_gate_count, path.gates.size, path.gates.size),
    kitShort(SkillTraining.kitFor(path)),
).joinToString(SEPARATOR)

/** Roughly how long, who it's for and what kit, for the program page. */
@Composable
fun programDetail(path: SkillPath): String = listOf(
    stringResource(R.string.program_weeks, path.weeks),
    stringResource(levelName(path.level)),
    kitShort(SkillTraining.kitFor(path)),
).joinToString(SEPARATOR)

/** "BEGINNER", "INTERMEDIATE" or "ADVANCED". */
@StringRes
fun levelName(level: SkillLevel): Int = when (level) {
    SkillLevel.BEGINNER -> R.string.program_level_beginner
    SkillLevel.INTERMEDIATE -> R.string.program_level_intermediate
    SkillLevel.ADVANCED -> R.string.program_level_advanced
}

/** The kit in a word or two each, or "NO KIT". */
@Composable
private fun kitShort(kit: Set<Equipment>): String = if (kit.isEmpty()) {
    stringResource(R.string.kit_short_none)
} else {
    Equipment.entries.filter { it in kit }.map { stringResource(kitShortName(it)) }.joinToString(" + ")
}

@StringRes
private fun kitShortName(equipment: Equipment): Int = when (equipment) {
    Equipment.PULL_UP_BAR -> R.string.kit_short_bar
    Equipment.LOW_BAR -> R.string.kit_short_low_bar
    Equipment.DIP_BARS -> R.string.kit_short_dip_bars
    Equipment.RINGS -> R.string.kit_short_rings
    Equipment.BAND -> R.string.kit_short_band
    Equipment.BOX -> R.string.kit_short_box
}

private const val SEPARATOR = " · "
