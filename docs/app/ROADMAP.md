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

## Phase 1: Design system and app skeleton

- `:design` module with the design tokens (colours, bundled Archivo and JetBrains Mono, brand text styles, spacing). Phone and watch themes are built from it. Done.
- Shared Gradle settings in convention plugins in `build-logic/`. Done.
- Hilt, Navigation 3 on both apps, and the ViewModel and UI state pattern on a first home screen. Done.
- Encrypted Room database (SQLCipher) with its key protected by the Android Keystore, failing closed when the key is missing. Done.
- CI runs the encryption tests on an emulator. Done.
- Test APKs of both apps built in CI on every push to `main`, signed with a shared test key, for installing without Android Studio. Done.
- Still to do: release app bundles in CI, and trying both apps on a real phone and watch.

**Done when** both apps open on an emulator with the real theme and read and write the encrypted database.

## Phase 2: The core loop on the phone (done, pending a try on a real phone)

- Goal picker: first pull-up and muscle-up paths. Done.
- Starting test: gate by gate, stopping at the first gate not passed. Done.
- Today's session: work sets at about 70% of the current best with 2 reps in reserve, then one best-effort set. Live logging with reps in reserve, and a rest timer that keeps time on the device's elapsed clock. Done.
- Path screen with every gate and the person's best. Done.
- Load modifiers: added weight (vest or belt) and bands per set, with live kilograms per rep. Bands are calibrated on a bathroom scale in the app. Band-assisted sets build toward a gate but don't pass it. Done.
- Bodyweight entry, kept as a log so Phase 4 can chart it. Done.

**Done when** someone can pick a goal, take the test and finish a full session on the phone.

## Phase 3: The watch

- Wear OS session screens: rep counter, hold timer, rest timer, and a load picker for vests, belts and bands. Done.
- Phone and watch sync over the Wearable Data Layer: the goal, bests, bands and the coach's next session go to the watch, finished workouts come back. Sync works offline and survives retries. Done.
- A session on the watch survives leaving the screen and the app being killed. Done.
- Health Services `ExerciseClient` for heart rate and an ongoing workout, in a `health` foreground service, with a rest alert that fires with the screen off. Built; not yet run on a real watch.
- A Tile showing the next session. Built; it still needs a preview image for the tile picker, taken from a real render.
- Still to do before calling the phase done: a full session on a real watch with the phone out of reach, then checking it arrives on the phone.

**Done when** a full session runs on the watch with the phone in a bag, and the results show up on the phone afterwards.

### Decisions to check on a real watch

These were made without a watch to test on. Each one has the result that would change it.

| Decision | Why | Change it if |
|---|---|---|
| The rest alert keeps the processor awake during rest with a short wake lock ([ADR 0006](adr/0006-watch-workout-service.md)). | It needs no setup from the person and always fires on time. An exact alarm needs a setting switched on from Android 14. | The buzz is late with the wrist down, or a session costs noticeably more battery than other workout apps. Then try a Health Services duration goal first, an exact alarm second. |
| The watch app is non-standalone: it needs the phone app. | The goal is chosen and the starting test is taken on the phone. A test on a watch screen would be slow and easy to get wrong. | People ask to set up on the watch alone. Then add a goal picker and starting test to the watch and mark it standalone. |
| The watch doesn't ask how hard a set felt. Watch sets are saved as "didn't say". | One more question between sets is friction on a small screen. The coach still raises a target when every work set hits it; it only can't tell "too easy" apart, so it moves up a step at a time instead of jumping. | People who train mostly on the watch progress noticeably slower than on the phone. Then ask on the rest screen, with one optional tap, so it never holds up a set. |
| The load is chosen once per session on the watch, in 1 kg steps, and goes on the skill work only. | Most people keep the same vest or band for their skill work. Loads for other exercises, per-set changes and half kilograms are on the phone. | Testing shows people want a load on other exercises, or change it between sets, on the watch. |
| The watch runs the plan as sent and doesn't offer the coach's "Lighter" choice. | The choice is made on the phone, where the reason is shown in full. A watch screen is too small to explain it well. | People start sessions on the watch without opening the phone and miss suggestions. Then show the suggestion on the watch with Keep and Lighter. |
| The screen stays on while the session screen is showing. | The hold timer runs on that screen and would be late if the watch dimmed. | Battery cost is noticeable. Then move the hold timer into the workout service and let the screen dim. |
| The Tile has no preview image for the tile picker yet. | It has to be a screenshot of the real Tile. A made-up picture would misrepresent it. | As soon as the Tile has been seen on a watch or emulator: take the screenshot, add round and square versions, and switch the lint check back on. |

## Next: the coach and the redesign

Trying the Phase 2 and 3 builds showed the app needs to work like a trainer, not a set of screens. Before Phase 4, in this order:

1. Redesign onboarding, Today, Train, program and workout screens in the design canvas, and get them approved.
2. Build the coach engine in `:core` ([ADR 0007](adr/0007-adaptive-coach.md), [COACH-ENGINE.md](COACH-ENGINE.md)), with its simulation tests. Done. Storing its model, a session run with more than one exercise, and reading weight from Health Connect come with step 3.
3. Rebuild the phone screens to the approved design. Setup, Today, the workout, Skills, the tab bar and the Train library (programs, single workouts, exercise detail) are done, driven by the coach. Still to come: the Health Connect weight reading and a weekly weigh-in prompt.
4. Bring the watch in line. Done: it runs the coach's sessions and sends finished workouts back to it.
5. Bundle exercise and onboarding photos.

Reading bodyweight from Health Connect moves here from Phase 4, because the coach uses the trend.

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
