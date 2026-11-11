# Architecture

How the mobile code is organised and the rules it follows. Keep this file up to date when the structure changes.

## Repository layout

```
mobile/
  settings.gradle.kts        module list and the only place repositories are declared
  build.gradle.kts           plugin versions, Spotless and detekt for every module
  gradle/libs.versions.toml  every dependency version
  build-logic/               convention plugins: shared Android, Kotlin, lint, R8 and Hilt settings
  config/detekt/             detekt rule changes
  core/                      Kotlin Multiplatform: training rules (no Android)
  design/                    design tokens: colours, fonts, type, spacing
  data/                      repositories, encrypted database, database key
  sync/                      phone and watch sync over the Wearable Data Layer
  testing/                   in-memory fakes of the repositories, for unit tests only
  app-phone/                 Android phone app (Compose, Navigation 3)
  app-wear/                  Wear OS app (Compose for Wear OS, Navigation 3)
```

Each app module is split by feature (`goal/`, `startingtest/`, `home/`, `session/`, `path/` on the phone; `home/`, `session/`, `load/` on the watch), plus `navigation/`, `theme/`, `sync/` and shared `ui/` components. A feature folder holds its screen, ViewModel and UI state.

Training logic that both apps need lives in `:core`, not in ViewModels. `SkillPath.trainingDay(bests)` works out the current gate and today's plan, and `SessionRun` holds the rules for moving through a session (log a set, rest, finish). ViewModels only combine flows and map the result to UI state.

### The coach

`app.levitx.core.coach` plans every session and learns from what was done ([ADR 0007](adr/0007-adaptive-coach.md), [COACH-ENGINE.md](COACH-ENGINE.md)). Its whole public surface is three calls on `Coach`:

- `recommend(model, now)` returns the next session, the reason for it, and an offer to ease off when there is one.
- `record(model, result)` returns the model after a finished session.
- `recordRest(model, now)` returns the model after the person chose to rest.

`Coach` holds no state. `AthleteModel` is immutable plain data that the app stores and passes back in, so the same model at the same time always gives the same answer. Both calls are pure calculation with no I/O. They take well under a millisecond, but ViewModels still call them off the main thread with everything else that isn't drawing.

Inside, each concern has one home:

| File | Owns |
|---|---|
| `CoachTuning` | Every number the coach decides with. Nothing else holds a threshold. |
| `ExerciseCatalogue`, `SkillTraining` | The exercises: one ladder per movement, and how each skill gate is trained. |
| `SessionBuilder` | The candidate sessions. It knows nothing about safety. |
| `HardRules` | The safety rules, and the repair that eases or cuts a session until it passes them. |
| `Scoring` | The six scoring terms and the scorer. |
| `Readiness` | The signals and the Keep / Lighter / Rest suggestion. |
| `Progression`, `LadderProgression`, `BlockKeeper` | How the model changes after a session: targets and rungs, gate bests, blocks. |
| `BodyweightTrend` | The weight trend and the fast-loss check. |
| `CoachReason`, `CoachPhrases` | Why a session was chosen, as data, and the English for it. |

A new safety rule is one object added to `HardRules.all`, and a new scoring term is one object added to `ScoringTerms.all`. Neither means changing the code that uses them. The rules and terms are `internal`, so the apps can't reach around `Coach`.

## Layers

We follow Android's [guide to app architecture](https://developer.android.com/topic/architecture):

| Layer | Object | Job |
|---|---|---|
| UI | Composable screen | Draws the UI state and passes user actions to its ViewModel. No logic. |
| UI | ViewModel | Turns repository data into one immutable UI state, exposed as a `StateFlow`. One per screen, injected with Hilt and scoped to its navigation entry. |
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
- **Link.** Phone and watch talk over the [Wearable Data Layer](https://developer.android.com/training/wearables/data/overview), through the `:sync` module. See [ADR 0003](adr/0003-phone-watch-communication.md).
- **Requirements.** Both apps use the application id `app.levitx` and must be signed with the same key.
- **Phone to watch.** The goal, bests and bands go to the watch as one data item. The watch copies them into its own database.
- **Watch to phone.** Each finished session is saved and queued on the watch in one transaction, sent as its own data item, and kept in the queue until the phone confirms it. Session ids are unique and stored once, so retries are harmless.
- **`:sync` is in three layers.** `PhoneSync` and `WatchSync` are what the apps call. The engines behind them hold the rules and are plain Kotlin. `PlayServicesDataLayer` is the only class that touches Google's API.

## A session on the watch

- **`WorkoutController` owns the session,** not a screen. It lives as long as the app process, so swiping back to the watch face mid-rest and returning lands on the same set. This follows the [Wear OS workout guidance](https://developer.android.com/health-and-fitness/health-services/active-data), which keeps workout state out of the activity.
- **Every change is saved as a draft** in the encrypted database, so a session survives the app being killed or crashing between sets.
- **The session id is made at the start.** If the app dies between the last set and the save, the restart saves it once, not twice.
- **Screens only watch and tap.** `SessionViewModel` maps the controller's state to UI state and passes actions on.
- **`WorkoutService` runs alongside,** a foreground service of type `health`. It reads heart rate through Health Services, shows the ongoing activity, and buzzes when rest ends. It watches the controller and stops when no session is running. The session doesn't depend on it. See [ADR 0006](adr/0006-watch-workout-service.md).

## The Tile

- **`NextSessionTileService`** shows the current gate and today's sets. It builds them from the same `trainingDay` as the home screen, so the two always agree.
- **It shows targets only,** never results or health data, because a Tile is visible to anyone who can see the watch.
- **`TileRefresher`** asks the system to redraw it when the goal or a best changes.

## Errors

- `:core` throws `IllegalArgumentException` for impossible input. The UI validates first, so a throw there is a bug, and tests cover it. Messages name the input but never its value, because values are often health data.
- Data that can't be unlocked raises `DataUnavailableException`. ViewModels turn that one exception into an error state with a plain message; any other exception is a bug and is allowed to crash, so it gets noticed and fixed rather than hidden.
- Messages and logs never contain the data itself.

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
- **Tests:** unit tests for `:core`, `:data` and every ViewModel.
- **Instrumented tests:** the Keystore and SQLCipher tests run on an Android emulator in CI.
- **Room schema:** each database version's schema must be committed.

## Testing

- `:core`: a unit test for every rule, including bad input.
- ViewModels: unit tests with fake repositories and `kotlinx-coroutines-test`.
- Repositories: fakes in `:testing` (`FakeGoalRepository`, `FakeProgressRepository`, `FakeSessionRepository` and others) keep state in memory, so ViewModel tests run on the JVM. Only `testImplementation` depends on them, so they never ship.
- Process death: a test rebuilds the session ViewModel from the same `SavedStateHandle` to check a session in progress survives the app being killed.
- Migrations: an emulator test upgrades a version 1 database to the latest and checks nothing was lost.
- Encryption: on an emulator, a test checks the database file doesn't start with SQLite's plain-text header, and that a tampered key file refuses to unlock.
- Sync: `:sync` unit tests run a pretend phone and a pretend watch against one in-memory Data Layer. They cover the full handshake, retries, the phone being out of reach, unreadable payloads and a second watch. Payload validation has its own tests with deliberately bad input.
- The watch session: unit tests cover the controller, including restarts mid-session.
- Not covered by automated tests: the real Data Layer between a real phone and watch, and the workout service (Health Services, the ongoing activity and the rest alert) on a real watch. Both need a manual check on devices before release.
