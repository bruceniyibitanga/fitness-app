package app.levitx.sync

/**
 * The id of a "delete everything" as it sits on the Data Layer: the UUID as
 * text. Like everything else that arrives, it is checked before it is used.
 */
internal object ResetCodec {
    /** The payload for [resetId]. */
    fun encode(resetId: String): ByteArray = resetId.encodeToByteArray()

    /** The reset id in [payload], or null unless it is exactly a UUID. */
    fun decodeOrNull(payload: ByteArray): String? = payload
        .takeIf { it.size <= MAX_ID_LENGTH }
        ?.decodeToString()
        ?.takeIf { UUID_PATTERN.matches(it) }
        ?.lowercase()
}
