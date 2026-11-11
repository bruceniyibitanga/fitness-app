package app.levitx.design

import androidx.compose.ui.graphics.Color

/**
 * The Levitx colour palette, the same values as the website's CSS variables
 * (`src/app/globals.css`).
 *
 * Dark only. Training happens in gyms and parks at all hours, and a dark UI
 * is easier to read at a glance between sets and kinder to OLED batteries.
 *
 * WCAG contrast against [Background]: [Text] 17.2:1, [TextSoft] 11.4:1,
 * [TextMuted] 7.4:1, [TextFaint] 4.6:1, [Accent] 6.9:1, [Ice] 9.1:1. All pass
 * AA (4.5:1) for normal text, though [TextFaint] only just, so keep it for
 * secondary details. [Tick] and [Line] are for graphics, never text.
 */
object LevitxColors {
    /** App background. */
    val Background = Color(0xFF0B0B0A)

    /** Raised surfaces: cards, sheets. */
    val Surface = Color(0xFF151513)

    /** Empty part of progress bars and sliders. */
    val Track = Color(0xFF1A1917)

    /** Dividers and outlines. */
    val Line = Color(0xFF24231F)

    /** Scale ticks and other fine marks. */
    val Tick = Color(0xFF3A3833)

    /** Outlines of unselected choices and future steps. A graphic, never text. */
    val Outline = Color(0xFF5A574F)

    /** Primary text. */
    val Text = Color(0xFFF2EFE8)

    /** Secondary text. */
    val TextSoft = Color(0xFFC9C5BB)

    /** Supporting text and labels. */
    val TextMuted = Color(0xFFA29E94)

    /** Secondary details. Passes AA, but only just. */
    val TextFaint = Color(0xFF7E7A71)

    /** Brand accent: the main action, progress and the current gate. */
    val Accent = Color(0xFFFF6A2B)

    /** Accent when pressed or highlighted. */
    val AccentHover = Color(0xFFFF8A57)

    /** Secondary accent for recovery, rest and calm states. */
    val Ice = Color(0xFF8DB4D9)
}
