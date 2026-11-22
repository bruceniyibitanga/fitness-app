package app.levitx.data.backup

import app.levitx.core.load.LoadLimits
import app.levitx.data.equipment.MAX_BAND_NAME_LENGTH

/* The single-value checks behind BackupDocument.checked. */

/** An id: present, not too long, and no control characters. */
internal fun String.isId(): Boolean = isNotBlank() && length <= MAX_ID_LENGTH && none { it.isISOControl() }

/** A band's name, as the Body tab accepts it. */
internal fun String.isBandName(): Boolean =
    trim().isNotEmpty() && length <= MAX_BAND_NAME_LENGTH && none { it.isISOControl() }

/** A bodyweight the app accepts. */
internal fun Double.isBodyweight(): Boolean = this in LoadLimits.MIN_BODYWEIGHT_KG..LoadLimits.MAX_BODYWEIGHT_KG

/** Added or assisting kilograms. NaN fails every comparison, so it's refused too. */
internal fun Double.isLoad(): Boolean = this in 0.0..MAX_LOAD_KG
