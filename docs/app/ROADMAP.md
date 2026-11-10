# App roadmap

The app ships in phases. Each phase has a clear finish line and doesn't start until the one before it passes CI and has been tried on a real device or emulator.

Android phone and Wear OS come first, in Kotlin with Jetpack Compose and Compose for Wear OS. iPhone and Apple Watch come after the Android release (see [ADR 0005](adr/0005-kotlin-compose-and-wear-compose.md)).

## Phase 0: Foundation (done)

- Gradle build in `mobile/` with a version catalog and a verified Gradle wrapper.
- `:core`, a Kotlin Multiplatform module holding the training rules, with tests:
  - load per rep, including vests, belts and bands
  - estimated one-rep max
  - pain-monitoring guidance
  - skill paths and gates
- `:app-phone` and `:app-wear` shells that open to a placeholder screen. Same application id, no permissions, no backup, R8 on for release.
- CI validates the wrapper and runs formatting, detekt, Android Lint, unit tests, and debug and release builds of both apps.
- Decision records, architecture notes and the security baseline written down.

**Done when** CI is green and these docs are reviewed.

## Phase 1: Design system and app skeleton (this phase)

- `:design` module with the design tokens (colours, bundled Archivo and JetBrains Mono, brand text styles, spacing). Phone and watch themes are built from it. Done.
- Shared Gradle settings in convention plugins in `build-logic/`. Done.
- Hilt, Navigation 3 on both apps, and the ViewModel and UI state pattern on a first home screen. Done.
- Encrypted Room database (SQLCipher) with its key protected by the Android Keystore, failing closed when the key is missing. Done.
- CI runs the encryption tests on an emulator. Done.
- Still to do: release app bundles in CI, and trying both apps on a real phone and watch.

**Done when** both apps open on an emulator with the real theme and read and write the encrypted database.

## Phase 2: The core loop on the phone

- Goal picker (muscle-up first) and the starting test.
- Today's session, live set logging with reps in reserve, and rest timer.
- Path screen with gates.
- Load modifiers: vest, belt and band (with band calibration).

**Done when** someone can pick a goal, take the test and finish a full session on the phone.

## Phase 3: The watch

- Wear OS session screens: rep counter, hold timer, rest timer.
- Health Services `ExerciseClient` for heart rate and an ongoing workout, in a `health` foreground service.
- Phone and watch sync over the Wearable Data Layer: the plan goes to the watch, finished sets come back. Sync works offline and survives retries.
- A Tile showing the next session.

**Done when** a full session runs on the watch with the phone in a bag, and the results show up on the phone afterwards.

## Phase 4: Body and progress

- Bodyweight log feeding load per rep.
- Strength trends: kg per rep and estimated max over time.
- Health Connect: read bodyweight, write workouts. Only the permissions actually used.
- Password-protected export and import.

## Phase 5: Pain-aware training

- Pain check-ins, the red-flag screen and swap suggestions.
- Physio plan entry by hand.
- Physio plan from a photo: text read on the device, every field confirmed by the user, photo deleted straight after.
- A physiotherapist reviews the rules and wording before this ships.

## Phase 6: Staying with it

- Comeback sessions instead of streaks, short sessions, if-then planning.
- Local notifications only.

## Phase 7: Release

- Accessibility, performance and battery passes on real devices.
- Security review against OWASP MASVS.
- Privacy policy, Play Console Data safety form, internal testing track.

## Later

- iPhone app (Compose Multiplatform or SwiftUI) and a SwiftUI Apple Watch app, both reusing `:core`.
- Optional sync between devices, if users ask for it.
