package app.levitx.data

import javax.inject.Qualifier

/**
 * Marks the dispatcher for blocking I/O (files, the Keystore, the database).
 *
 * Injected rather than referenced directly so tests can swap in a test
 * dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
