package app.levitx.phone.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import app.levitx.phone.BuildConfig
import app.levitx.phone.R

/** Where feedback goes: the address the privacy policy gives. */
internal const val FEEDBACK_ADDRESS = "support@xenovadigital.com"

/** The longest a phone's own description of itself is allowed to be in the draft. */
internal const val MAX_DEVICE_TEXT = 40

/** Blank lines above the footer, where the person writes. */
private const val WRITING_SPACE = "\n\n\n"

/**
 * A value the phone reports about itself (its model, its Android version),
 * made safe to put in an email draft: control characters and line breaks
 * become spaces, runs of spaces become one, and it is cut to
 * [MAX_DEVICE_TEXT]. A phone that reports nothing gives "unknown".
 */
internal fun deviceText(raw: String?): String {
    val cleaned = raw.orEmpty()
        .map { if (it.isISOControl()) ' ' else it }
        .joinToString("")
        .trim()
        .replace(Regex("\\s+"), " ")
        .take(MAX_DEVICE_TEXT)
        .trim()
    return cleaned.ifEmpty { "unknown" }
}

/** The draft's starting text: room to write, then [footer] on its own line. */
internal fun feedbackBody(footer: String): String = WRITING_SPACE + footer

/**
 * Opens an email draft to [FEEDBACK_ADDRESS] in the person's own email app.
 *
 * The draft carries the app's version, the Android version and the phone's
 * model, all in plain sight where the person can delete them. Nothing about
 * their training, their body or their settings goes in, and nothing is sent
 * until they press send in their email app.
 *
 * `ACTION_SENDTO` with a `mailto:` uri is what Android's common intents guide
 * gives for an email with no attachment, so only email apps answer it.
 *
 * @return False when the phone has no email app to open.
 */
internal fun sendFeedback(context: Context): Boolean {
    val footer = context.getString(
        R.string.settings_feedback_footer,
        BuildConfig.VERSION_NAME,
        deviceText(Build.VERSION.RELEASE),
        deviceText(Build.MODEL),
    )
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_ADDRESS))
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_feedback_subject))
        putExtra(Intent.EXTRA_TEXT, feedbackBody(footer))
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        // No email app. Settings then shows the address to write to by hand.
        false
    }
}
