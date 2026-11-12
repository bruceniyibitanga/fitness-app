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

Each app module is split by feature (`onboarding/`, `main/`, `today/`, `workout/`, `train/`, `exercise/`, `program/`, `path/`, `skills/`, `equipment/` on the phone; `home/`, `session/`, `load/` on the watch), plus `navigation/`, `theme/`, `sync/` and shared `ui/` components. A feature folder holds its screen, ViewModel and UI state.

Training logic lives in `:core`, not in ViewModels. The coach plans every session on the phone, and `WorkoutRun` holds the rules for moving through one (log a set, rest, skip, finish). The phone and the watch both run sessions with it. ViewModels only combine flows and map the result to UI state.

### The coach

`app.levitx.core.coach` plans every session and learns from what was done ([ADR 0007](adr/0007-adaptive-coach.md), [COACH-ENGINE.md](COACH-ENGINE.md)). Its whole public surface is four calls on `Coach`:

- `recommend(model, now)` returns the next session, the reason for it, and an offer to ease off when there is one.
- `record(model, result)` returns the model after a finished session.
- `recordRest(model, now)` returns the model after the person chose to rest.
- `workout(model, focus, now)` returns a workout the person picked from the Train tab (full body, upper, pull, push, legs or core). It is built by the same builder and passes the same hard rules as the coach's own sessions, so a movement trained too recently comes back light.

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

The phone stores the coach through `CoachRepository` in `:data`. What the coach has learned is one JSON document in the `coach_state` table, with its own format version. It is a separate type from `AthleteModel` (`CoachDocument`), so the stored format only changes on purpose. Bests and bodyweight aren't in it: they stay in their own tables, which the path screen, the bodyweight screen and the watch sync already read, and the repository fills them into the model when it reads it. Every change is a read, a call to `Coach` and a write in one transaction. A finished workout is stored as one `training_session` row per exercise, sharing a `workoutId`, so it can't be recorded twice.

`WorkoutRun` in `:core` runs a planned session exercise by exercise. The workout in progress is saved after every change in `workout_draft`, so it survives the app being killed. `SessionRun`, which ran one gate at a time, is only kept so the watch can save a gate session an older version left in progress, and so the phone can read gate sessions from a watch that hasn't been updated.

### The phone app

The phone's main destinations on its Navigation 3 back stack are setup, the tabs, and the workout in progress, with detail screens (an exercise, a program, Progress, History, a past workout) opened on top of the tabs. Which comes first is decided once at launch: setup for someone the coach doesn't know yet, the tabs for everyone else. Locked or unreadable data goes to the tabs, which explain it, so setup can never overwrite a coach that is still there.

- **Setup** (`onboarding/`) asks only what training can't tell the coach: the goal, all-round or focused, whether they can do a pull-up, optional bodyweight, the week and the kit. It shows the first session the coach would plan before anything is saved, then saves it all in one go.
- **The tabs** (`main/`) are Today, Train, Plan, Skills and Body. They swap content in place, so back from any tab leaves the app.
- **Today** asks `Coach.recommend` for the next session and shows the reason and the Keep / Lighter / Rest offer. Starting saves a `WorkoutRun` as the draft before the workout screen opens, so the draft is the one place the workout in progress lives. Starting again resumes it.
- **The workout** reads the draft, saves it after every change, and hands the finished run to `CoachRepository.recordWorkout`. Draft writes go through one lock, in order, and recording waits behind them, so an older write can't bring a finished workout back.
  - Holds and slow negatives have a timer (`SetTimer`). It is worked out from the elapsed clock alone, so a late frame or a trip to the background can't put it out. It isn't saved with the draft: if the app is closed mid-hold the set is still there to do, which is the safe way round. Stopping it fills in the set for the person to check and log.
  - Rest counts down, beeps and then waits. It never starts the next set on its own. It can be made 15 seconds longer or shorter, and the set that started it can be taken back (`WorkoutRun.undoLastSet`).
  - The vest, belt or band is kept in the run, not the screen, so it carries from set to set and survives a timer and the app being closed.
  - Beeps use `ToneGenerator` on the media stream, made off the main thread. They can be turned off from the session menu for the rest of the workout. The phone is usually on the floor during a set, so sound carries the timing and the buzz is extra.
  - The summary (`SessionReport`) compares the coach's model before and after it learned from the workout, so "what changes next time" is what the coach actually changed.
  - Gates (`WorkoutGates.kt`): a set that would pass the current gate waits as a `PendingSet` until the person confirms it met the standard, then the screen marks the pass (`GatePass`) before the session carries on. The coach still raises the gate's best only when the workout is recorded, the same way as before; the phone keeps its own copy of the bests during the workout so the same gate isn't checked twice. A planned attempt (`ExercisePlan.isGateAttempt`) gets a short brief first, and can be put off with `WorkoutRun.deferAttempt`, which is saved with the draft.
- **Skills** shows where the person is on every ladder, read straight from the coach's model through `currentExercise` and `currentGatePattern`, so it can't disagree with what Today plans.
- **Train** lists the skill programs and one workout per focus. A program can be made the plan, which changes only the goal. A workout opens a detail screen and starts through `WorkoutStarter`, the same path Today uses, so a workout in progress is never replaced.
- **Exercise detail** opens from Today, a workout or Skills: form cues and set-up notes from `ExerciseGuides` in `:core`, the person's target, best and load per rep, and the exercise's ladder with their place on it.
- **Plan** shows the goal's gates on a rail, with how many are left and what each one trains.
- **Body** shows the bodyweight trend from `bodyweightTrend` in `:core` (the same figure the coach plans with), the load per rep it gives each exercise trained now, the kit the plan can use, and the bands. Kit changes go through `CoachRepository.updateProfile`, so Today plans with the new kit straight away.
- **Progress** opens from Today's figures and from Plan, not a tab of its own, because it's something to check weekly rather than daily. It reads every finished workout through `HistoryRepository` (`:data`), which groups the stored rows into `WorkoutRecord`s off the main thread. The figures come from `core/history`: `Progress.loadSeries` for kilograms per rep over time (a square marks each climb to a harder rung), `visits` and the records for one exercise, `workoutsPerWeek`, and `passedGates`, dated by the first stored workout that reached the target. Hard sets this week come from the coach's own model and window, so Progress and the coach never count the week differently. Nothing on these screens is estimated: an exercise without a load figure, or a workout without a bodyweight, simply has no kilograms.
- **History** shows a month at a time in the phone's time zone, with trained days filled and training days still to come outlined, and opens each **past workout** with every set as it was logged.

#### The design kit

Every screen is built to its artboard in the "Levitx App Design" canvas, from one small kit in `app-phone/ui/`:

- `Bars.kt`: the square orange `AccentBar` for the main action (label left, arrow right), and the quieter `QuietBar` and `OutlineBar`. There are no rounded buttons anywhere.
- `Type.kt` and `Headers.kt`: mono captions, the tab header with the orange mark, the step header for setup, and `PhotoHeader` for detail screens.
- `Blocks.kt`, `Chips.kt`, `ChoiceStyle.kt`, `KitPicker.kt`, `GateRow.kt`, `SessionList.kt`: the stat strip, photo tiles, numbered rows, chips, choice bars, the kit picker and the gate rail, each shared by every screen that needs it.
- `Photos.kt`: which bundled photo goes with which program, workout focus and movement, so the same thing always has the same picture. The photos are listed with their sources in `docs/app/PHOTO-CREDITS.md`.

Colours, fonts and type sizes come from `:design` (`LevitxColors`, `LevitxFonts`, `LevitxType`), which match the artboards' values.

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

- **Who does what.** The coach runs on the phone, where the person's history is. The watch runs the session the coach planned, on its own once it has it, which [Google recommends for Wear OS apps](https://developer.android.com/training/wearables/apps/standalone-apps). The phone is where planning, history and settings live.
- **Link.** Phone and watch talk over the [Wearable Data Layer](https://developer.android.com/training/wearables/data/overview), through the `:sync` module. See [ADR 0003](adr/0003-phone-watch-communication.md).
- **Requirements.** Both apps use the application id `app.levitx` and must be signed with the same key.
- **Phone to watch.** The goal, bests, bands, the next session the coach planned and the ids of the last workouts the coach recorded go to the watch as one data item (`PhoneStateSource` puts it together). The watch copies it into its own database (`PhoneStateApplier`). Only the plan crosses; the coach's model stays on the phone.
- **Watch to phone.** Finishing a workout on the watch is one transaction: it is queued, the watch's copy of the bests is raised, the draft is cleared, and the plan is set aside while the watch waits for the phone. Each workout is sent as its own data item and kept in the queue until the phone confirms it. The phone records it with the coach (`CoachRepository.recordWorkout`), which stores a workout id once, so retries are harmless. With no coach set up yet, the phone leaves workouts unconfirmed so none are lost.
- **No session twice.** After a workout, the watch doesn't take a plan until the phone's recent workout ids include it. A plan made before the phone had the workout could be the very session just done.
- **Older apps.** Gate sessions queued by an older watch app are still sent and received on their old path, so updating one device before the other loses nothing. The wire format is at version 2; the state is only read by a phone and watch that both have it.
- **`:sync` is in three layers.** `PhoneSync` and `WatchSync` are what the apps call. The engines behind them hold the rules and are plain Kotlin. `PlayServicesDataLayer` is the only class that touches Google's API.

## A session on the watch

- **It is the coach's session,** exercise by exercise, run by the same `WorkoutRun` as the phone. The watch shows the exercise, the set and the target, times rest, and lets the person skip an exercise or end early. It doesn't offer the coach's "Lighter" choice: that is made on the phone, which then sends the lighter plan.
- **The load chosen on the watch is for the skill work,** as it was when the watch ran one gate. Other exercises are logged at bodyweight; their loads are set on the phone.
- **`WorkoutController` owns the session,** not a screen. It lives as long as the app process, so swiping back to the watch face mid-rest and returning lands on the same set. This follows the [Wear OS workout guidance](https://developer.android.com/health-and-fitness/health-services/active-data), which keeps workout state out of the activity.
- **Every change is saved as a draft** in the encrypted database, so a session survives the app being killed or crashing between sets.
- **The workout id is made at the start.** If the app dies between the last set and the save, the restart saves it once, not twice.
- **Screens only watch and tap.** `SessionViewModel` maps the controller's state to UI state and passes actions on.
- **`WorkoutService` runs alongside,** a foreground service of type `health`. It reads heart rate through Health Services, shows the ongoing activity, and buzzes when rest ends. It watches the controller and stops when no session is running. The session doesn't depend on it. See [ADR 0006](adr/0006-watch-workout-service.md).

## The Tile

- **`NextSessionTileService`** shows the next session: the first exercise, how many exercises and roughly how long. It uses the same summary as the home screen, so the two always agree.
- **It shows the plan only,** never results or health data, because a Tile is visible to anyone who can see the watch.
- **`TileRefresher`** asks the system to redraw it when the plan changes.

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
- **Screen previews:** the `screenshots` job renders every preview in `app-phone/src/screenshotTest` to an image at the artboards' 390 × 844 size, using Compose Preview Screenshot Testing, and uploads them as the `phone-screens` artifact. It doesn't compare against stored images yet; it exists so each screen can be checked against its artboard on every push.

## Testing

- `:core`: a unit test for every rule, including bad input.
- ViewModels: unit tests with fake repositories and `kotlinx-coroutines-test`.
- Repositories: fakes in `:testing` (`FakeGoalRepository`, `FakeProgressRepository`, `FakeSessionRepository` and others) keep state in memory, so ViewModel tests run on the JVM. Only `testImplementation` depends on them, so they never ship.
- Process death: the workout in progress is saved as an encrypted draft after every change, and the workout tests check the draft matches what is on screen after each step.
- Migrations: an emulator test upgrades a version 1 database to the latest and checks nothing was lost.
- Encryption: on an emulator, a test checks the database file doesn't start with SQLite's plain-text header, and that a tampered key file refuses to unlock.
- Sync: `:sync` unit tests run a pretend phone and a pretend watch against one in-memory Data Layer. They cover the full handshake, retries, the phone being out of reach, unreadable payloads and a second watch. Payload validation has its own tests with deliberately bad input.
- The watch session: unit tests cover the controller, including restarts mid-session, skipping, the load, and a gate session left by an older version.
- Not covered by automated tests: the real Data Layer between a real phone and watch, and the workout service (Health Services, the ongoing activity and the rest alert) on a real watch. Both need a manual check on devices before release.
