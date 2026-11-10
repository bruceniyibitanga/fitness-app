# 0004: MVVM from the Flutter guide, plus a pure Dart core package

**Status:** Accepted, November 2026

## Context

We want a structure that's easy to test, easy to follow, and the same in both apps. Flutter's [architecture guide](https://docs.flutter.dev/app-architecture/guide) recommends MVVM: views, view models, repositories and services. It mentions an optional domain layer for complex logic.

## Decision

- **Follow the guide** in both apps. View models use `ChangeNotifier` and commands, as in the guide's case study.
- **Shared rules go in `training_core`,** a pure Dart package with no Flutter imports and no I/O. That covers load per rep, strength estimates, pain guidance and skill paths.
- **No state-management framework** until a real need shows up. The guide's built-in approach is enough to start.

## Consequences

- **Fast tests.** The training rules are tested without a device and run in milliseconds in CI.
- **One copy of the rules.** The phone and the watch can't disagree about a number.
- **Some boilerplate.** We accept a little extra code per screen (a view model each) in exchange for screens that are testable.
