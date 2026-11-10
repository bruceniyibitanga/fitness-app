package app.levitx.testing

import app.levitx.core.session.SessionRun
import app.levitx.data.session.SessionDraftRepository

/** In-memory [SessionDraftRepository]. */
class FakeSessionDraftRepository(initial: SessionRun? = null) : SessionDraftRepository {
    /** The draft as last saved, for assertions. */
    var current: SessionRun? = initial
        private set

    override suspend fun load(): SessionRun? = current

    override suspend fun save(run: SessionRun) {
        current = run
    }

    override suspend fun clear() {
        current = null
    }
}
