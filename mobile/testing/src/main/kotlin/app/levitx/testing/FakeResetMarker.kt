package app.levitx.testing

import app.levitx.data.privacy.ResetMarker

/** In-memory [ResetMarker]. */
class FakeResetMarker(initial: String? = null) : ResetMarker {
    /** The id held now, for assertions. */
    var id: String? = initial
        private set

    override suspend fun current(): String? = id

    override suspend fun set(resetId: String) {
        id = resetId
    }
}
