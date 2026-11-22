# Security and privacy baseline

The app handles health information: pain ratings, injuries, physio plans and bodyweight. Under the Australian Privacy Act, health information is sensitive information. We treat it that way from the first line of code.

## Principles

1. **Keep it on the device.** No accounts, no server and no cloud database in the early phases. Nothing to breach centrally.
2. **Collect less.** If a feature works without a piece of data, we don't ask for it.
3. **No third-party tracking.** No analytics, ad or crash SDKs that send data off the device. If we add crash reporting later, it gets its own ADR and is opt-in.
4. **Ask at the moment of need.** Permissions are requested when a feature needs them, with a sentence on why. Never all at once at launch.
5. **Fail closed.** If the database key can't be read, the app shows an error. It never falls back to an unencrypted or empty database.

## What we store and how

| Data | Class | Where | Protection |
|---|---|---|---|
| Pain check-ins and warning signs | Health | Not stored. Used once to work out the advice for the area, then dropped. While the check is open they are held in memory only (never in saved state) and the screen is marked secure | Nothing to protect at rest. The advice itself is in the row below |
| Physio plans (not built yet) | Health | Local database | Encrypted database, key in Android Keystore |
| Bodyweight (a log of readings), progress photos | Health | Local database / app-private files | Encrypted |
| Bands and the help they give | Personal | Local database | Encrypted |
| Sets, reps, holds, sessions | Personal | Local database | Encrypted (one database, one policy) |
| What the coach has learned (training history, targets, latest pain advice, how suggestions went) and the workout in progress | Health | Local database, as JSON documents in two tables | Encrypted. Never synced. Reading a damaged document fails without quoting it, so none of it reaches a crash report |
| The watch's copy of the goal, bests, bands and next session, and its own workouts | Personal | The watch's local database | Encrypted the same way, with the watch's own Keystore key |
| Heart rate | Health | Watch memory only, while a session runs | Never written to disk, never sent to the phone or anywhere else |
| Settings: the count-in, sounds and buzzes | Low | A DataStore file in the app's private storage | Not encrypted, because none of it is personal. Excluded from backups like everything else |
| Database key | Secret | `noBackupFilesDir`, encrypted with a Keystore AES-GCM key | Never in code, logs or backups |

The database key is 256 random bits made on the device the first time the app runs (`DatabaseKeyStore` in `:data`):

- It is encrypted with AES-256-GCM using a Keystore key that can't be exported, held in StrongBox on devices that have it and in the Trusted Execution Environment otherwise.
- The encrypted key lives in `noBackupFilesDir`, which no backup ever includes. GCM authenticates it, so a changed file fails to unlock instead of producing a wrong key.
- SQLCipher gets it as a raw key, so there is no password stretching to slow down app start (the key is random, not a password).
- **Fail closed.** If the key is missing, tampered with, or its Keystore key is gone, or if the database exists without a key, the app shows an error. It never creates a fresh key over existing data, and never falls back to an unencrypted database.
- These behaviours are tested on an emulator in CI.

## The person's own copy, and deleting

- **Export.** Settings > Export everything writes a zip with `levitx.json` (all of it, including the coach's stored state) and `workouts.csv` and `weigh-ins.csv` for a spreadsheet. The person picks where it goes in the system's save dialog, so the app needs no storage permission and can write only that one file. The file is **not encrypted**: it's for the person to read and use elsewhere, and the screen says so and asks them to keep it somewhere private. The password-protected file for moving to a new phone (below) is a separate thing and comes with an import.
- **Delete one workout.** Removes its rows and sets, takes it out of the coach's recent history, and puts any best it set back to the best of the workouts left (`Forgetting` in `:core`). Targets the coach moved since stay; they rest on later workouts too, so undoing them would be a guess.
- **Delete everything.** Settings > Delete everything, after a confirm sheet. Room deletes every row, sets a WAL checkpoint and runs `VACUUM`, so the space the rows used is rewritten. The settings file is cleared, and the phone takes its published state off the Data Layer. Once begun, the delete runs to the end even if the screen is closed. Each delete has a random id, saved before anything is sent and kept on the Data Layer for good. The next time the watch is connected it sees an id it hasn't acted on and deletes everything it holds the same way, queued workouts included, and removes its own items from the Data Layer. That holds even if the watch only reconnects after the person has set up again, and if the id couldn't be sent at the time, it is sent the next time the app starts. The id is random and says nothing about the person; it is the one thing in the settings file that a delete keeps. Uninstalling the phone app doesn't reach the watch; the privacy policy says to clear Levitx there too. The app then starts again from setup with a fresh plan.

## Backups

- **Nothing is backed up or transferred automatically.** Both apps set `allowBackup="false"` and ship data extraction rules that exclude everything from cloud backup and from device-to-device transfer. From Android 12, `allowBackup` alone doesn't stop device transfer, so the rules are needed too.
- **Why.** The database can only be decrypted with a Keystore key, and Keystore keys never leave the device. A backup would be unreadable on a new phone, and keeping one would only add another copy of health data to worry about. [Android's guidance](https://developer.android.com/privacy-and-security/risks/backup-best-practices) is to keep sensitive data out of backups.
- **Moving to a new phone** works through a password-protected export the person makes themselves (Phase 4). See [ADR 0002](adr/0002-local-first-encrypted-storage.md).

## Phone and watch traffic

- **Who can read it.** Android's docs say Data Layer traffic [may at some point use Google-owned servers](https://developer.android.com/training/wearables/data/overview), though no other app can read it. Play services only delivers data between copies of this app with the same package name and signing key.
- **What goes over it.** Only what the watch needs to run a session, and what comes back:
  - phone to watch: which goal is active, the best per gate, the bands (name and kilograms of help), the next session the coach planned (exercises, sets, targets and rest), and the ids of the last ten workouts the coach recorded
  - watch to phone: finished workouts (the plan they ran, start and finish times, each set's reps or seconds and load, and which exercises were skipped), and gate sessions from an older watch app
- **What never goes over it.** The coach's model (training history, pain advice, how suggestions went), bodyweight, pain notes, physio documents and photos. They stay on the phone. Only the plan the coach produced goes to the watch. Tests check that bodyweight is not in a session's payload.
- **How payloads are handled.** Nothing that arrives is trusted:
  - Every payload has a version number and a size limit.
  - Ids, counts and values are checked against fixed limits before anything reaches the database.
  - Anything that fails is skipped whole, and the log gets a count, never the contents.
- **The listener services are exported,** because Google Play services has to be able to start them. `WearableListenerService` only accepts calls from Play services, and each service's filter only matches our own paths.

## Photos and text recognition (Phase 5)

- Text is read on the device. No image or text is sent anywhere.
- We only extract the fields we need (exercises, sets, reps, limits). The user confirms each one.
- The photo is deleted as soon as the user confirms, and names and other identifiers are not kept.

## Feedback

- Settings has a "Send feedback" row. It opens a draft in the person's own email app, addressed to support. Levitx sends nothing itself: the phone app has no internet permission to send with.
- The draft carries three things: the app version, the Android version and the phone's model. They sit in plain sight at the bottom of the draft, and the person can delete them before sending.
- Nothing about training, bodyweight, pain or settings goes in. The model and Android version are stripped of line breaks and control characters and cut to 40 characters, so a phone can't add lines to the draft.
- If the phone has no email app, Settings shows the address to write to instead.

## Code and build

- **Logs.** No personal data in logs. Log codes and counts, never values.
- **Release builds.** R8 shrinking, optimisation and obfuscation are on, and CI builds the release variant on every push. SQLCipher's classes are kept from renaming in `:data`'s consumer rules, because its native code looks them up by name.
- **Fonts and other assets** are bundled, so the apps make no network requests to show text.
- **Signing keys.** Kept outside the repo; `.gitignore` blocks keystores and `key.properties`. Releases use Play App Signing: Google holds the app signing key, and we sign uploads with an upload key Google can replace. The upload key is a secret of the `release` GitHub environment, which only the hand-run, main-only release workflow can use, after approval. The release build doesn't use the Gradle cache. See [RELEASE.md](RELEASE.md).
- **Permissions are pinned.** CI reads the permissions out of both release builds, including any a library adds, and fails if they differ from the lists in `mobile/config/permissions`.
- **The test signing key** signs the APKs CI builds for device testing, so the phone and watch builds match. It lives only in two repository secrets, is written to the runner's temp folder for the build, and is only used by the `Mobile test build` workflow, which runs on `main` and on demand, never on pull requests. It is not the release key. If it leaks, the worst case is someone making an APK that can install over a test build, so it gets replaced rather than worked around.
- **Dependencies.** Kept few and reviewed before adding. Every version is pinned in `libs.versions.toml`, and Dependabot opens update PRs weekly.
- **Repositories.** Declared once in `settings.gradle.kts`. Modules can't add their own, and Google's repository may only serve Google and AndroidX groups, so a look-alike package can't be pulled from the wrong place.
- **Gradle wrapper.** The wrapper jar is checked against Gradle's published checksums in CI, and the distribution download is checked against its SHA-256.
- **Permissions.** Each one has a comment in the manifest saying which feature needs it. The phone app has these, all for keeping a workout going off screen ([ADR 0008](adr/0008-phone-workout-off-screen.md)):
  - notifications: the workout notification with the rest countdown. Asked for once, when the first workout opens. A workout runs and beeps without it.
  - Live Updates (`POST_PROMOTED_NOTIFICATIONS`): lets that notification sit on the lock screen and in the status bar on Android 16. Granted at install.
  - foreground service (type `health`), wake lock and vibrate: granted at install. They keep the workout running with the phone locked, and give the beeps and buzzes.
  - `HIGH_SAMPLING_RATE_SENSORS`: granted at install. Android 14 and later only start a `health` service for an app with one sensor permission, and this is the only one that isn't a prompt. Levitx reads no sensors.

  The watch app has these:
  - heart rate (`BODY_SENSORS` up to Wear OS 5, `READ_HEART_RATE` from Wear OS 6): shown during a session. Asked for when the first session starts. A session runs without it.
  - notifications: the "session in progress" notification. Asked for at the same time.
  - foreground service (type `health`), vibrate and wake lock: granted at install. They keep the session going off-screen and give the rest alert ([ADR 0006](adr/0006-watch-workout-service.md)).
- **The workout notifications are public** (they show on a locked watch or phone). The watch's only ever says "Exercise 1 of 4, set 2 of 3" or "Rest". The phone's adds the exercise name, a progress bar and the rest or hold clock. Neither shows reps, loads, bodyweight or anything else from the log.
- **CI.** Uses a read-only token and actions pinned to commit SHAs.
- **Release checklist.** [OWASP MASVS](https://mas.owasp.org/MASVS/), worked through before release: see [MASVS-REVIEW.md](MASVS-REVIEW.md).

## Reporting a problem

If you find a security issue, don't open a public issue. Contact the maintainer directly.
