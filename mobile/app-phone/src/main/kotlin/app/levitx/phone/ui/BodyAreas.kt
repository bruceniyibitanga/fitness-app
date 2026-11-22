package app.levitx.phone.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.levitx.core.coach.BodyArea
import app.levitx.core.pain.PainAdvice
import app.levitx.design.LevitxColors
import app.levitx.phone.R

/** A body area's name, like "Lower back". */
@Composable
fun areaName(area: BodyArea): String = stringResource(
    when (area) {
        BodyArea.WRIST -> R.string.area_wrist
        BodyArea.ELBOW -> R.string.area_elbow
        BodyArea.SHOULDER -> R.string.area_shoulder
        BodyArea.LOWER_BACK -> R.string.area_lower_back
        BodyArea.HIP -> R.string.area_hip
        BodyArea.KNEE -> R.string.area_knee
        BodyArea.ANKLE -> R.string.area_ankle
    },
)

/**
 * What the coach is doing about a sore area, in a few capitals, or null when
 * it trains as planned.
 */
@Composable
fun soreStatus(advice: PainAdvice?): String? = when (advice) {
    PainAdvice.EASE_OFF -> stringResource(R.string.pain_status_eased)
    PainAdvice.SEE_PROFESSIONAL -> stringResource(R.string.pain_status_paused)
    PainAdvice.CONTINUE_AS_PLANNED, null -> null
}

/** The colour of [soreStatus]: orange for an area taken out of the plan, ice blue for one on light work. */
fun soreColor(advice: PainAdvice?): Color =
    if (advice == PainAdvice.SEE_PROFESSIONAL) LevitxColors.Accent else LevitxColors.Ice
