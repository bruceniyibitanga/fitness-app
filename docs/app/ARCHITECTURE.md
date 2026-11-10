# Architecture

How the mobile code is organised and the rules it follows. Keep this file up to date when the structure changes.

## Repository layout

```
mobile/
  pubspec.yaml            workspace root: one lockfile for everything
  analysis_options.yaml   shared lint rules
  packages/
    training_core/        pure Dart training logic (Phase 0)
  apps/
    phone/                Flutter Android app (Phase 1)
    watch/                Flutter Wear OS app (Phase 1)
```

The workspace uses [pub workspaces](https://dart.dev/tools/pub/workspaces), which need Dart 3.6 or later. Every member shares one dependency resolution, so the phone and watch can't end up on different versions of the same package.

## Layers

We follow Flutter's [app architecture guide](https://docs.flutter.dev/app-architecture/guide). It describes MVVM with four kinds of object:

| Layer | Object | Job |
|---|---|---|
| UI | View | A widget tree. Shows state and passes taps to its view model. No logic. |
| UI | View model | Turns repository data into UI state and exposes actions as commands. One per view. |
| Data | Repository | The source of truth for one kind of data. Caching, retries and error handling live here. |
| Data | Service | A thin wrapper around one outside thing: the database, secure storage, the Data Layer, Health Services. |

On top of the guide we add a domain package, `training_core`. It's pure Dart with no Flutter and no I/O, so the phone and the watch run the same maths and it can be tested in milliseconds.

Dependencies only point one way:

```
View -> View model -> Repository -> Service
                 \-> training_core <-/
```

`training_core` depends on nothing in the app.

## SOLID and DRY, without overdoing it

- **Single responsibility.** One file, one job. A view never talks to a service.
- **Open for extension.** New exercises, gates and red flags are data in a catalogue, not new branches in code.
- **Interfaces where there is a real second implementation,** such as a fake service in tests or a different platform. We don't add an interface just in case.
- **Sealed classes for closed sets,** like load modifiers, so the compiler flags any place that forgets a case.
- **One source for every number.** Bodyweight shares live in `LoadCatalogue`. Pain limits live in `pain_guidance.dart`. Nothing is copied into the UI.

## Async and performance

The UI must never wait on I/O or heavy work.

- All I/O is `async`: database, secure storage, platform channels and the Data Layer.
- The database runs on a background isolate. Drift supports this; confirm the exact API against the drift docs in Phase 1.
- Heavy work, like rebuilding a year of trends, runs in `Isolate.run`.
- Kotlin bridges use Pigeon's `@TaskQueue` so native work stays off the platform's main thread.
- Live data (heart rate, rep counts, sync state) is exposed as streams, and every subscription is cancelled when its owner is disposed. The `cancel_subscriptions` lint enforces this.
- Every `Future` is awaited or explicitly ignored. The `unawaited_futures` lint enforces this.

## Phone and watch

- **Who does what.** The watch can run a whole session on its own, which [Google recommends for Wear OS apps](https://developer.android.com/training/wearables/apps/standalone-apps). The phone is where planning, history and settings live.
- **Link.** Phone and watch talk over the [Wearable Data Layer](https://developer.android.com/training/wearables/data/overview), the only supported channel between them.
- **Requirements.** Both apps need the same package name and the same signing key. Data Layer messages don't work otherwise.
- **The plan goes to the watch as a data item.** Data items are delivered when the watch is next reachable, even if it's offline now.
- **Finished sets come back as messages.** They're kept in a local outbox until the phone acknowledges them. Each set has a unique ID, so a retried message is harmless.
- **Workout sensors on the watch** come from [Health Services](https://developer.android.com/training/wearables/health-services) (`ExerciseClient`), not raw sensors. It's more accurate and easier on the battery.
- **No Flutter plugin covers these Android APIs officially,** so we write small Kotlin bridges generated with [Pigeon](https://docs.flutter.dev/interop/pigeon), the Flutter team's recommended tool. They give type-safe calls and no hand-written channel names.

## Errors

- Repositories return a `Result` (ok or error) instead of throwing, as the Flutter case study does. The UI shows a plain message and logs a code, never the data.
- `training_core` throws `ArgumentError` for impossible input. The UI validates first, so a throw there is a bug, and tests cover it.

## Documentation and comments

- Every public class, function and field is documented. The `public_member_api_docs` lint is set to error, so undocumented code fails CI.
- Comments explain *why*, not *what*. Where a number comes from research, the comment names the source.
- Big decisions get an ADR in `docs/app/adr/`.

## Testing

- `training_core`: unit tests for every rule, including bad input.
- View models: unit tests with fake repositories.
- Repositories: tests against fake services.
- Kotlin bridges: tested on an emulator in Phase 3.
- CI runs format, analyze and test on every push to `mobile/`.
