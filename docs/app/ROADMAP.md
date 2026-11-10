# App roadmap

The app ships in phases. Each phase has a clear finish line and doesn't start until the one before it passes CI and has been tried on a real device or emulator.

Android phone and Wear OS come first, built with Flutter. iPhone and Apple Watch come after the Android release (see [ADR 0001](adr/0001-flutter-for-android-and-wear-os.md)).

## Phase 0: Foundation (this phase)

- Dart workspace in `mobile/` with one shared package, `training_core`.
- `training_core` holds the training maths, with tests:
  - load per rep, including vests, belts and bands
  - estimated one-rep max
  - pain-monitoring guidance
  - skill paths and gates
- CI checks formatting, analysis and tests on every push.
- Decision records, architecture notes and the security baseline written down.

**Done when** CI is green and these docs are reviewed.

## Phase 1: App shells

- Generate the phone app and the Wear OS app with `flutter create`. Both get the same application ID and signing key, which the Wearable Data Layer requires.
- Theme and design tokens from the design system (colours, Archivo and JetBrains Mono, square edges).
- MVVM skeleton following Flutter's architecture guide: views, view models, repositories, services.
- Navigation and dependency injection.
- Encrypted local database, with its key held by the Android Keystore.
- Backup rules, so health data only ever leaves the phone end-to-end encrypted.
- CI builds a release app bundle for both apps.

**Done when** both apps open on an emulator and read and write the encrypted database.

## Phase 2: The core loop on the phone

- Goal picker (muscle-up first) and the starting test.
- Today's session, live set logging with reps in reserve, and rest timer.
- Path screen with gates.
- Load modifiers: vest, belt and band (with band calibration).

**Done when** someone can pick a goal, take the test and finish a full session on the phone.

## Phase 3: The watch

- Wear OS session screens: rep counter, hold timer, rest timer.
- Health Services `ExerciseClient` for heart rate and an ongoing workout, through a small Kotlin bridge generated with Pigeon.
- Phone and watch sync over the Wearable Data Layer: the plan goes to the watch, finished sets come back. Sync works offline and survives retries.

**Done when** a full session runs on the watch with the phone in a bag, and the results show up on the phone afterwards.

## Phase 4: Body and progress

- Bodyweight log feeding load per rep.
- Strength trends: kg per rep and estimated max over time.
- Health Connect: read bodyweight, write workouts. Only the permissions actually used.

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

- iPhone app from the same Flutter code, plus a native SwiftUI Apple Watch app.
- Optional sync between devices, if users ask for it.
