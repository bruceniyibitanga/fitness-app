package app.levitx.data.backup

import java.security.SecureRandom

/**
 * The six words that lock a backup (docs/app/adr/0009-encrypted-backup-and-restore.md).
 *
 * Levitx makes them, never the person: words drawn at random from the EFF
 * large word list (7,776 words, CC BY 3.0 US, credited in the app's notices)
 * give about 77.5 bits, out of reach of guessing however long someone has
 * the file. A password people choose for themselves is usually far weaker.
 */
public object RecoveryWords {
    /** Words in a set. */
    public const val COUNT: Int = 6

    /** Words in the list, as the EFF publishes it. */
    internal const val LIST_SIZE: Int = 7_776

    private const val LIST_PATH = "/app/levitx/data/backup/eff_large_wordlist.txt"

    /**
     * The word list, read once from the bundled file. Fails loudly if the
     * file isn't the list it should be, because a shorter or changed list
     * would quietly make every backup weaker or impossible to open.
     */
    internal val list: List<String> by lazy {
        val stream = checkNotNull(RecoveryWords::class.java.getResourceAsStream(LIST_PATH)) { "word list missing" }
        val words = stream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readLines().filter { it.isNotBlank() }
        }
        check(words.size == LIST_SIZE && words.toSet().size == LIST_SIZE) { "word list has the wrong number of words" }
        check(words.all { word -> word.isNotEmpty() && word.all { it in 'a'..'z' || it == '-' } }) { "word list has unexpected characters" }
        words
    }

    private val known: Set<String> by lazy { list.toHashSet() }

    /** [COUNT] words picked uniformly at random by [random], which must be a [SecureRandom]. */
    public fun generate(random: SecureRandom = SecureRandom()): List<String> =
        List(COUNT) { list[random.nextInt(LIST_SIZE)] }

    /**
     * The words in [typed], or null unless it is exactly [COUNT] words from the
     * list. Case, spaces, line breaks and commas between words don't matter,
     * so the words can be typed the way they were written down.
     */
    public fun parse(typed: String): List<String>? {
        val words = typed.lowercase().split(Separators).filter { it.isNotEmpty() }
        return words.takeIf { it.size == COUNT && it.all(known::contains) }
    }

    /** True when [word] is on the list: lets a screen mark a mistyped word as it is typed. */
    public fun isWord(word: String): Boolean = word.trim().lowercase() in known

    private val Separators = Regex("[\\s,]+")
}
