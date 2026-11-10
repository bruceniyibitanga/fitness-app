# Mobile apps

Android phone and Wear OS apps, written in Flutter, in one Dart workspace.

Start with these:

- [Roadmap](../docs/app/ROADMAP.md): what gets built in which phase
- [Architecture](../docs/app/ARCHITECTURE.md): how the code is organised
- [Security](../docs/app/SECURITY.md): how health data is protected
- [Decisions](../docs/app/adr/README.md): why things are the way they are

## What's here now (Phase 0)

`packages/training_core` is the training logic both apps will share. It's pure Dart:

- **Load per rep:** `calculateEffectiveLoad`, `LoadCatalogue`, vests, belts and bands.
- **Estimated one-rep max:** `estimateOneRepMax`, the Epley formula with reps in reserve.
- **Pain guidance:** `assessPain`, the pain-monitoring model with red flags.
- **Skill paths:** `SkillPath`, `SkillCatalogue.muscleUp`.

The phone and watch apps arrive in Phase 1.

## Running it

You need Dart 3.13 or later. Flutter includes it.

```bash
cd mobile
dart pub get
dart format .
dart analyze --fatal-infos
cd packages/training_core && dart test
```

After your first `dart pub get`, commit the generated `pubspec.lock`. It pins every dependency version for everyone.

CI runs the same checks on every push that touches `mobile/`.
