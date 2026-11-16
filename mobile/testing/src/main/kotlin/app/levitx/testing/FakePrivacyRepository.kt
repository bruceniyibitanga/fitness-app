package app.levitx.testing

import app.levitx.data.privacy.PrivacyRepository
import java.io.OutputStream

/**
 * [PrivacyRepository] that counts deletions. [onDeleteEverything] runs on each
 * one, so a test can empty its other fakes the way the real database is emptied.
 */
class FakePrivacyRepository(private val onDeleteEverything: suspend () -> Unit = {}) : PrivacyRepository {
    /** How many times everything was deleted. */
    var deletions = 0
        private set

    override suspend fun export(out: OutputStream) {
        out.write("{}".toByteArray())
    }

    override suspend fun deleteEverything() {
        deletions++
        onDeleteEverything()
    }
}
