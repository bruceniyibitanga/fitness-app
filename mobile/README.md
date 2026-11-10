# Mobile apps

The Levitx Android phone app and Wear OS watch app, in Kotlin with Jetpack Compose and Compose for Wear OS, plus the training rules they share.

Start with these:

- [Roadmap](../docs/app/ROADMAP.md): what gets built in which phase
- [Architecture](../docs/app/ARCHITECTURE.md): how the code is organised
- [Security](../docs/app/SECURITY.md): how health data is protected
- [Decisions](../docs/app/adr/README.md): why things are the way they are

## What's here

- **`core/`**: the training rules, in plain Kotlin with tests:
  - load per rep (`calculateEffectiveLoad`, `LoadCatalogue`), with vests, belts and bands
  - estimated one-rep max (`estimateOneRepMax`), the Epley formula with reps in reserve
  - pain guidance (`assessPain`), the pain-monitoring model with red flags
  - skill paths (`SkillPath`, `SkillCatalogue`)
  - session plans and the rules for moving through one (`SessionPlanner`, `SessionRun`)
- **`design/`**: colours, fonts, brand text styles and spacing, shared by both apps.
- **`data/`**: the repositories both apps read and write through, backed by an encrypted Room database whose key is protected by the Android Keystore.
- **`sync/`**: keeps the phone and watch in step over the Wearable Data Layer. The goal, bests and bands go to the watch; finished sessions come back.
- **`testing/`**: in-memory fakes of the repositories, for unit tests.
- **`app-phone/`**: the phone app. Goal picker, starting test, sessions, the path, and bodyweight and bands.
- **`app-wear/`**: the watch app. Runs today's session from the wrist: rep counter, hold timer, rest timer.
- **`build-logic/`**: Gradle convention plugins with the settings every Android module shares.

## Setting up

1. Install [Android Studio](https://developer.android.com/studio) (latest stable). It includes the JDK and Android SDK.
2. Open the `mobile/` folder, not the repo root. Android Studio reads the Gradle build from there.
3. Let the Gradle sync finish. The wrapper downloads the right Gradle version and checks its checksum.
4. Pick `app-phone` or `app-wear` in the run menu, and a phone or Wear OS emulator.

### Trying phone and watch sync

Sync needs a phone and a watch that are paired with each other. On emulators:

1. Create a phone emulator with the Play Store, and a Wear OS emulator.
2. Pair them in Android Studio's Device Manager (the phone emulator's menu has "Pair Wearable").
3. Install `app-phone` on the phone and `app-wear` on the watch. Both are debug builds from the same machine, so they share a signing key, which the Data Layer requires.
4. Choose a goal and take the starting test on the phone. The goal should appear on the watch within a few seconds.
5. Run a session on the watch. It should show up in the phone's progress, and "1 session waiting for your phone" on the watch should clear.

To watch sync happen, filter Logcat by the `LevitxSync` tag on either device. It logs counts only, never data.

To pair a phone emulator with a watch emulator, use Android Studio's **Device Manager > Pair Wearable**.

## Checks

These are what CI runs. Run them before pushing:

```bash
cd mobile
./gradlew spotlessApply            # fix formatting
./gradlew detekt                   # code smells and missing docs
./gradlew check                    # lint and unit tests
./gradlew assembleDebug assembleRelease
```

`./gradlew :core:jvmTest` runs just the training-rule tests, which takes a few seconds.

The encryption tests need a running emulator or a connected device:

```bash
./gradlew :data:connectedDebugAndroidTest
```

When you change the database, bump its version in `LevitxDatabase`, add a migration, build once, and commit the new schema file Room writes to `data/schemas/`. CI fails if you forget.

## Adding a dependency

Add the version and library to `gradle/libs.versions.toml`, then reference it as `libs.<name>` in the module. Check it is actively maintained, from a source we trust, and needed. Say why in the pull request.
