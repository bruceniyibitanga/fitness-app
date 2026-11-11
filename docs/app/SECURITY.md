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
| Pain check-ins, red flags, physio plans | Health | Local database | Encrypted database, key in Android Keystore |
| Bodyweight (a log of readings), progress photos | Health | Local database / app-private files | Encrypted |
| Bands and the help they give | Personal | Local database | Encrypted |
| Sets, reps, holds, sessions | Personal | Local database | Encrypted (one database, one policy) |
| What the coach has learned (training history, targets, latest pain advice, how suggestions went) and the workout in progress | Health | Local database, as JSON documents in two tables | Encrypted. Never synced. Reading a damaged document fails without quoting it, so none of it reaches a crash report |
| The watch's copy of the goal, bests and bands, and its own sessions | Personal | The watch's local database | Encrypted the same way, with the watch's own Keystore key |
| Heart rate | Health | Watch memory only, while a session runs | Never written to disk, never sent to the phone or anywhere else |
| Settings | Low | Local database | Encrypted |
| Database key | Secret | `noBackupFilesDir`, encrypted with a Keystore AES-GCM key | Never in code, logs or backups |

The database key is 256 random bits made on the device the first time the app runs (`DatabaseKeyStore` in `:data`):

- It is encrypted with AES-256-GCM using a Keystore key that can't be exported, held in StrongBox on devices that have it and in the Trusted Execution Environment otherwise.
- The encrypted key lives in `noBackupFilesDir`, which no backup ever includes. GCM authenticates it, so a changed file fails to unlock instead of producing a wrong key.
- SQLCipher gets it as a raw key, so there is no password stretching to slow down app start (the key is random, not a password).
- **Fail closed.** If the key is missing, tampered with, or its Keystore key is gone, or if the database exists without a key, the app shows an error. It never creates a fresh key over existing data, and never falls back to an unencrypted database.
- These behaviours are tested on an emulator in CI.

## Backups

- **Nothing is backed up or transferred automatically.** Both apps set `allowBackup="false"` and ship data extraction rules that exclude everything from cloud backup and from device-to-device transfer. From Android 12, `allowBackup` alone doesn't stop device transfer, so the rules are needed too.
- **Why.** The database can only be decrypted with a Keystore key, and Keystore keys never leave the device. A backup would be unreadable on a new phone, and keeping one would only add another copy of health data to worry about. [Android's guidance](https://developer.android.com/privacy-and-security/risks/backup-best-practices) is to keep sensitive data out of backups.
- **Moving to a new phone** works through a password-protected export the person makes themselves (Phase 4). See [ADR 0002](adr/0002-local-first-encrypted-storage.md).

## Phone and watch traffic

- **Who can read it.** Android's docs say Data Layer traffic [may at some point use Google-owned servers](https://developer.android.com/training/wearables/data/overview), though no other app can read it. Play services only delivers data between copies of this app with the same package name and signing key.
- **What goes over it.** Only what the watch needs to run a session, and what comes back:
  - phone to watch: which goal is active, the best per gate, and the bands (name and kilograms of help)
  - watch to phone: finished sessions (the gate, start and finish times, and each set's reps or seconds and load)
- **What never goes over it.** Bodyweight, pain notes, physio documents and photos. They stay on the phone. A test checks that bodyweight is not in a session's payload.
- **How payloads are handled.** Nothing that arrives is trusted:
  - Every payload has a version number and a size limit.
  - Ids, counts and values are checked against fixed limits before anything reaches the database.
  - Anything that fails is skipped whole, and the log gets a count, never the contents.
- **The listener services are exported,** because Google Play services has to be able to start them. `WearableListenerService` only accepts calls from Play services, and each service's filter only matches our own paths.

## Photos and text recognition (Phase 5)

- Text is read on the device. No image or text is sent anywhere.
- We only extract the fields we need (exercises, sets, reps, limits). The user confirms each one.
- The photo is deleted as soon as the user confirms, and names and other identifiers are not kept.

## Code and build

- **Logs.** No personal data in logs. Log codes and counts, never values.
- **Release builds.** R8 shrinking, optimisation and obfuscation are on, and CI builds the release variant on every push. SQLCipher's classes are kept from renaming in `:data`'s consumer rules, because its native code looks them up by name.
- **Fonts and other assets** are bundled, so the apps make no network requests to show text.
- **Signing keys.** Kept outside the repo; `.gitignore` blocks keystores and `key.properties`. Release builds will use Play App Signing.
- **The test signing key** signs the APKs CI builds for device testing, so the phone and watch builds match. It lives only in two repository secrets, is written to the runner's temp folder for the build, and is only used by the `Mobile test build` workflow, which runs on `main` and on demand, never on pull requests. It is not the release key. If it leaks, the worst case is someone making an APK that can install over a test build, so it gets replaced rather than worked around.
- **Dependencies.** Kept few and reviewed before adding. Every version is pinned in `libs.versions.toml`, and Dependabot opens update PRs weekly.
- **Repositories.** Declared once in `settings.gradle.kts`. Modules can't add their own, and Google's repository may only serve Google and AndroidX groups, so a look-alike package can't be pulled from the wrong place.
- **Gradle wrapper.** The wrapper jar is checked against Gradle's published checksums in CI, and the distribution download is checked against its SHA-256.
- **Permissions.** The phone app has none. The watch app has these, each with a comment in the manifest saying which feature needs it:
  - heart rate (`BODY_SENSORS` up to Wear OS 5, `READ_HEART_RATE` from Wear OS 6): shown during a session. Asked for when the first session starts. A session runs without it.
  - notifications: the "session in progress" notification. Asked for at the same time.
  - foreground service (type `health`), vibrate and wake lock: granted at install. They keep the session going off-screen and give the rest alert ([ADR 0006](adr/0006-watch-workout-service.md)).
- **The workout notification is public** (it shows on a locked watch) and only ever says "Set 2 of 4" or "Rest".
- **CI.** Uses a read-only token and actions pinned to commit SHAs.
- **Release checklist.** [OWASP MASVS](https://mas.owasp.org/MASVS/), worked through before release.

## Reporting a problem

If you find a security issue, don't open a public issue. Contact the maintainer directly.
