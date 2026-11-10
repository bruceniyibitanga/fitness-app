package app.levitx.data

import javax.inject.Qualifier

/**
 * Marks the coroutine scope that lives as long as the app process.
 *
 * Use it for work that must finish even if the screen that started it goes
 * away, such as saving a set or syncing with the other device. Injected
 * rather than created in place, so tests can pass their own scope and nothing
 * needs `GlobalScope`.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
