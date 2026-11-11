# 0005: Kotlin, Jetpack Compose and Compose for Wear OS

**Status:** Accepted, November 2026. Supersedes [0001](0001-flutter-for-android-and-wear-os.md).

## Context

The watch is central to Levitx, not an add-on. A session on the watch needs Health Services for heart rate and an ongoing workout, the Wearable Data Layer to sync with the phone, a foreground service so the workout survives the screen turning off, and later Tiles. All of these are Android APIs with official Kotlin libraries and no official Flutter plugins.

With Flutter we would have written Kotlin bridges for every one of them, so the watch would have been part Dart and part Kotlin anyway. Google's own recommendation for Wear OS UI is [Compose for Wear OS](https://developer.android.com/training/wearables/compose), and Horologist (Google's Wear OS helper libraries) is Kotlin only.

The maintainer asked for the best result and developer experience without compromise, and accepted learning Kotlin to get it.

## Decision

- **Kotlin everywhere on Android.** Jetpack Compose for the phone, Compose for Wear OS with Material 3 for the watch.
- **Shared rules in a Kotlin Multiplatform module, `:core`.** It is plain Kotlin with no Android imports. Today it only has a JVM target, which both apps use directly. iOS and watchOS targets get added when that work starts.
- **One Gradle build in `mobile/`** with a version catalog (`gradle/libs.versions.toml`) for every version, and AGP 9's built-in Kotlin support.
- **Same application id, `app.levitx`, for phone and watch.** The Data Layer requires it, along with the same signing key.
- **iPhone and Apple Watch later.** The iPhone app will use either Compose Multiplatform (stable on iOS since 1.8) or SwiftUI; we decide when we start, based on how mature Compose Multiplatform is then. The Apple Watch app will be SwiftUI. Both reuse `:core`, which compiles for iOS and watchOS without changes.

## Consequences

- **Direct access to every platform API** we need, through Google's own libraries, with no bridge code to maintain.
- **One copy of the training rules** for phone, watch and, later, Apple devices.
- **A new language for the maintainer.** Kotlin reads closely to TypeScript and Dart, and the docs, comments and ADRs are written to make the code easy to learn from.
- **The shared module only stays shared if it stays pure.** Anything that needs Android (storage, sensors, sync) lives in the apps, behind interfaces where `:core` needs to call it.
- **Shared build settings live in convention plugins** in `build-logic/` (`levitx.android.application`, `levitx.android.library`, `levitx.hilt`), as Android's [Now in Android](https://github.com/android/nowinandroid) sample does. A module's build file only says what is different about it.
