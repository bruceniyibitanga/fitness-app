package app.levitx.phone.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import app.levitx.core.history.WeekNote
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.patternName

/*
 * How the week review words each note, and the colour of its bullet:
 * orange for something about the person's training, ice blue for the block.
 */

/** A note in a sentence. */
@Composable
internal fun noteText(note: WeekNote): String = when (note) {
    is WeekNote.ShortOf -> pluralStringResource(
        R.plurals.review_note_short,
        note.sets,
        note.sets,
        patternName(note.pattern),
    )

    is WeekNote.LighterWeekDue -> pluralStringResource(R.plurals.review_note_lighter_due, note.sessions, note.sessions)

    is WeekNote.LighterWeekNow ->
        pluralStringResource(R.plurals.review_note_lighter_now, note.sessionsLeft, note.sessionsLeft)
}

/** The bullet's colour. */
internal fun noteColor(note: WeekNote): Color = when (note) {
    is WeekNote.ShortOf -> LevitxColors.Accent
    is WeekNote.LighterWeekDue, is WeekNote.LighterWeekNow -> LevitxColors.Ice
}
