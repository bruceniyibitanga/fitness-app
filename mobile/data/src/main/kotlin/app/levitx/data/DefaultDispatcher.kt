package app.levitx.data

import javax.inject.Qualifier

/**
 * Marks the dispatcher for CPU work, such as turning stored rows into
 * workouts, so it never runs on the main thread.
 *
 * Injected rather than referenced directly so tests can swap in a test
 * dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
