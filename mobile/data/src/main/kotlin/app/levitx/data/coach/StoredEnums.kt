package app.levitx.data.coach

/**
 * Reads an enum stored by name, or null when this version of the app doesn't
 * know the name. Stored documents keep enums as names so a value added later
 * can be dropped by an older app instead of making the whole document unreadable.
 */
internal inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? = enumValues<T>().firstOrNull { it.name == this }

/** Reads a map keyed by enum names, dropping keys this version doesn't know. */
internal inline fun <reified K : Enum<K>, V> Map<String, V>.withEnumKeys(): Map<K, V> =
    entries.mapNotNull { (key, value) -> key.toEnumOrNull<K>()?.let { it to value } }.toMap()

/** Writes a map keyed by enums with the names as keys. */
internal fun <K : Enum<K>, V> Map<K, V>.withNameKeys(): Map<String, V> = mapKeys { it.key.name }
