# 0001: Flutter for the Android phone and Wear OS apps

**Status:** Accepted, November 2026

## Context

We're starting with an Android phone app and a Wear OS watch app. The maintainer knows TypeScript and Flutter well, and wants to avoid codebases in languages he doesn't know. We looked at three options:

- **Expo (React Native).** Expo doesn't manage Wear OS as a target. React Native doesn't officially support Wear OS either (no round-screen components, for example). The watch app would have to be Kotlin and Jetpack Compose, so the watch would be in a language the maintainer doesn't know, and the training logic would be written twice.
- **Native Kotlin.** Best platform access, but a new language for the whole project.
- **Flutter.** To Flutter, a Wear OS app is just an Android app, so the phone and the watch can both be Dart and share one logic package. Flutter has no first-party Wear OS support, so round screens and ambient mode come from community packages, and a few platform APIs need small Kotlin bridges.

## Decision

Flutter for both apps, in one Dart workspace, sharing `training_core`.

## Consequences

- One language for nearly everything, and one copy of the training maths.
- **Small Kotlin bridges for platform APIs** with no maintained Flutter plugin: Health Services and the Wearable Data Layer. We generate them with Pigeon so they're type-safe. They're small, and each one is documented.
- **Watch-specific widgets come from community packages.** We'll check each one for maintenance before adding it. Tiles and complications, if we want them, would need Kotlin.
- **Apple Watch later still needs a SwiftUI app.** Flutter can't run on watchOS. The iPhone app can reuse the Flutter code.
