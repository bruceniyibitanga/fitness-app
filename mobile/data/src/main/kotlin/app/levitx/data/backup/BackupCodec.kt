package app.levitx.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The most a backup's contents may unpack to. Real backups are a few
 * megabytes of JSON; the cap stops a small file that unpacks to gigabytes.
 */
internal const val MAX_CONTENTS_BYTES = 64 * 1024 * 1024

private const val BUFFER_BYTES = 8 * 1024

/**
 * Turns a [BackupDocument] into the bytes that get encrypted, and back:
 * JSON, gzipped. Compressing before encrypting is safe here: nobody can feed
 * chosen text into a backup and watch its size, which is what such attacks need.
 */
internal object BackupCodec {
    // Unknown keys are ignored so the version check, not a parse error, turns away a newer backup.
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(document: BackupDocument): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(json.encodeToString(BackupDocument.serializer(), document).toByteArray()) }
        return out.toByteArray()
    }

    /**
     * The document in [bytes].
     *
     * @throws BackupException with [BackupProblem.UNREADABLE_CONTENTS] when
     *   it isn't gzipped JSON of the right shape, or unpacks past [MAX_CONTENTS_BYTES].
     */
    fun decode(bytes: ByteArray): BackupDocument {
        val text = try {
            unzip(bytes).toString(Charsets.UTF_8)
        } catch (error: IOException) {
            fail(BackupProblem.UNREADABLE_CONTENTS, error)
        }
        return try {
            json.decodeFromString(BackupDocument.serializer(), text)
        } catch (error: SerializationException) {
            fail(BackupProblem.UNREADABLE_CONTENTS, error)
        } catch (error: IllegalArgumentException) {
            fail(BackupProblem.UNREADABLE_CONTENTS, error)
        }
    }

    /** [bytes] unpacked, read a buffer at a time and stopped as soon as it passes the cap. */
    private fun unzip(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            var read = input.read(buffer)
            while (read >= 0) {
                out.write(buffer, 0, read)
                if (out.size() > MAX_CONTENTS_BYTES) fail(BackupProblem.UNREADABLE_CONTENTS)
                read = input.read(buffer)
            }
        }
        return out.toByteArray()
    }
}
