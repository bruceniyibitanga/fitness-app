# Architecture

How the mobile code is organised and the rules it follows. Keep this file up to date when the structure changes.

## Repository layout

```
mobile/
  settings.gradle.kts      module list and the only place repositories are declared
  build.gradle.kts         plugin versions, Spotless and detekt for every module
  gradle/libs.versions.toml  every dependency version
  config/detekt/           detekt rule changes
  core/                    Kotlin Multiplatform: training rules (no Android)
  app-phone/               Android phone app (Compose)
  app-wear/                Wear OS app (Compose for Wear OS)
```

Phase 1 adds `design/` (shared design tokens) and `build-logic/` (shared Gradle settings).

## Layers

We follow Android's [guide to app architecture](https://developer.android.com/topic/architecture):

| Layer | Object | Job |
|---|---|---|
| UI | Composable screen | Draws the UI state and passes user actions to its ViewModel. No logic. |
| UI | ViewModel | Turns repository data into one immutable UI state, exposed as a `StateFlow`. One per screen. |
| Domain | `:core` | The training rules. Pure Kotlin, no I/O, shared by both apps and later by iOS. |
| Data | Repository | The source of truth for one kind of data. Caching, retries and error handling live here. |
| Data | Data source | A thin wrapper around one outside thing: the database, the Keystore, the Data Layer, Health Services. |

Dependencies only point one way:

```
Screen -> ViewModel -> Repository -> Data source
                  \-> :core <-/
```

`:core` depends on nothing in the apps.

## SOLID and DRY, without overdoing it

- **Single responsibility.** One file, one job. A screen never talks to a data source.
- **Open for extension.** New exercises, gates and red flags are data in a catalogue, not new branches in code.
- **Interfaces where there is a real second implementation,** such as a fake for tests or a different platform. We don't add an interface just in case.
- **Sealed types for closed sets,** like load modifiers, so the compiler flags any `when` that forgets a case.
- **One source for every number.** Bodyweight shares live in `LoadCatalogue`, pain limits in `PainGuidance.kt`, versions in `libs.versions.toml`. Nothing is copied.
- **Invalid states can't be built.** Types like `LoadProfile` and `PainCheckIn` check their values when they are created, so the rest of the code never has to.

## Async and performance

The main thread only draws. It never waits on I/O or heavy work.

- **Coroutines and Flow everywhere.** One-off work is a `suspend` function; anything that changes over time (heart rate, rep counts, sync state, database queries) is a `Flow`.
- **The right dispatcher at the edge.** Data sources move blocking work to `Dispatchers.IO` (or `Default` for CPU work) themselves, so callers can always call them from the main thread. The dispatcher is injected so tests can replace it.
- **Lifecycle-aware collection.** Screens use `collectAsStateWithLifecycle`, so collection stops when the app goes to the background.
- **Structured concurrency.** Work runs in `viewModelScope` or a scope the owner cancels. No `GlobalScope`.
- **Watch battery.** Sensors come from Health Services, which batches readings, rather than raw sensor listeners.

## Phone and watch

- **Who does what.** The watch can run a whole session on its own, which [Google recommends for Wear OS apps](https://developer.android.com/training/wearables/apps/standalone-apps). The phone is where planning, history and settings live.
- **Link.** Phone and watch talk over the [Wearable Data Layer](https://developer.android.com/training/wearables/data/overview). See [ADR 0003](adr/0003-phone-watch-communication.md).
- **Requirements.** Both apps use the application id `app.levitx` and must be signed with the same key.
- **The plan goes to the watch as a data item; finished sets come back as messages** held in an outbox until acknowledged. Set IDs are unique, so retries are harmless.

## Errors

- `:core` throws `IllegalArgumentException` for impossible input. The UI validates first, so a throw there is a bug, and tests cover it. Messages name the input but never its value, because values are often health data.
- Repositories return a result type instead of throwing for expected failures (no connection, permission denied). The UI shows a plain message and logs a code, never the data.

## Documentation and comments

- Every public class, function and property has KDoc. detekt's `Undocumented*` rules fail CI otherwise, and `:core` uses Kotlin's explicit API mode so nothing becomes public by accident.
- Comments explain *why*, not *what*. Where a number comes from research, the comment names the source.
- Big decisions get an ADR in `docs/app/adr/`.

## Code quality checks

All run in CI on every push that touches `mobile/`, and all fail the build:

- **Formatting:** Spotless with ktlint (Android Studio style). `./gradlew spotlessApply` fixes it.
- **Static analysis:** detekt, with documentation rules turned on.
- **Android Lint:** warnings are errors.
- **Compiler warnings** are errors.
- **Tests:** `:core` unit tests, and from Phase 1 ViewModel tests.

## Testing

- `:core`: a unit test for every rule, including bad input.
- ViewModels: unit tests with fake repositories and `kotlinx-coroutines-test`.
- Repositories: tests against fake data sources.
- Health Services and the Data Layer: tested on emulators in Phase 3, using Health Services' synthetic data on the Wear OS emulator.
