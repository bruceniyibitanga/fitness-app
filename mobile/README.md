# Mobile apps

The Levitx Android phone app and Wear OS watch app, in Kotlin with Jetpack Compose and Compose for Wear OS, plus the training rules they share.

Start with these:

- [Roadmap](../docs/app/ROADMAP.md): what gets built in which phase
- [Architecture](../docs/app/ARCHITECTURE.md): how the code is organised
- [Security](../docs/app/SECURITY.md): how health data is protected
- [Decisions](../docs/app/adr/README.md): why things are the way they are

## What's here now (Phase 0)

- **`core/`** holds the training rules, in plain Kotlin with tests:
  - load per rep (`calculateEffectiveLoad`, `LoadCatalogue`), with vests, belts and bands
  - estimated one-rep max (`estimateOneRepMax`), the Epley formula with reps in reserve
  - pain guidance (`assessPain`), the pain-monitoring model with red flags
  - skill paths (`SkillPath`, `SkillCatalogue.muscleUp`)
- **`app-phone/`** and **`app-wear/`** are the app shells. They open to a placeholder screen. Real screens start in Phase 1.

## Setting up

1. Install [Android Studio](https://developer.android.com/studio) (latest stable). It includes the JDK and Android SDK.
2. Open the `mobile/` folder, not the repo root. Android Studio reads the Gradle build from there.
3. Let the Gradle sync finish. The wrapper downloads the right Gradle version and checks its checksum.
4. Pick `app-phone` or `app-wear` in the run menu, and a phone or Wear OS emulator.

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

## Adding a dependency

Add the version and library to `gradle/libs.versions.toml`, then reference it as `libs.<name>` in the module. Check it is actively maintained, from a source we trust, and needed. Say why in the pull request.
