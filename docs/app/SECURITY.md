# Security and privacy baseline

The app handles health information: pain ratings, injuries, physio plans and bodyweight. Under the Australian Privacy Act, health information is sensitive information. We treat it that way from the first line of code.

## Principles

1. **Keep it on the device.** No accounts, no server and no cloud database in the early phases. Nothing to breach centrally.
2. **Collect less.** If a feature works without a piece of data, we don't ask for it.
3. **No third-party tracking.** No analytics, ad or crash SDKs that send data off the device. If we add crash reporting later, it gets its own ADR and is opt-in.
4. **Ask at the moment of need.** Permissions are requested when a feature needs them, with a sentence on why. Never all at once at launch.
5. **Fail closed.** If the database key can't be read, the app shows an error. It never falls back to an unencrypted database.

## What we store and how

| Data | Class | Where | Protection |
|---|---|---|---|
| Pain check-ins, red flags, physio plans | Health | Local database | Encrypted database, key in Android Keystore |
| Bodyweight, progress photos | Health | Local database / app-private files | Encrypted |
| Sets, reps, holds, sessions | Personal | Local database | Encrypted (one database, one policy) |
| Settings | Low | Local database | Encrypted |
| Database key | Secret | Android Keystore-backed secure storage | Never in code, logs or backups |

The database key is 256 random bits made on the device the first time the app runs.

## Backups

- **What Android does.** Auto Backup can copy app data to the user's Google Drive. [Android's guidance](https://developer.android.com/privacy-and-security/risks/backup-best-practices) is to leave sensitive data out, or to require end-to-end encryption. On Android 9 and later with a screen lock set, backups are encrypted with a key Google doesn't know.
- **Our rule.** App data goes into a backup only when that client-side encryption is on (`requireFlags="clientSideEncryption"`).
- **Open question for Phase 1.** Keystore keys can't be exported, so a backed-up database restored on a new phone couldn't be decrypted. The options are:
  - leave the database out of backup and offer an explicit, password-protected export
  - or wrap the database key in a way that survives a restore

  We'll decide with an ADR before any real data is stored.

## Phone and watch traffic

- **Who can read it.** Android's docs say Data Layer traffic [may at some point use Google-owned servers](https://developer.android.com/training/wearables/data/overview), though no other app can read it.
- **What goes over it.** We only send what the watch needs to run a session: the plan, and the sets that come back.
- **What never goes over it.** Pain notes, physio documents and photos. They stay on the phone.
- **How payloads are handled.** Every payload has a version number and is validated on arrival. Anything unexpected is dropped and logged without its contents.

## Photos and text recognition (Phase 5)

- Text is read on the device. No image or text is sent anywhere.
- We only extract the fields we need (exercises, sets, reps, limits). The user confirms each one.
- The photo is deleted as soon as the user confirms, and names and other identifiers are not kept.

## Code and build

- **Logs.** No personal data in logs. Log codes and counts, never values.
- **Release builds.** Code shrinking and obfuscation are on.
- **Signing keys.** Kept outside the repo; `.gitignore` blocks keystores and `key.properties`. Use Play App Signing.
- **Dependencies.** Kept few and reviewed before adding. Versions are locked by `pubspec.lock`, and Dependabot opens update PRs weekly.
- **CI.** Uses a read-only token and actions pinned to commit SHAs.
- **Release checklist.** [OWASP MASVS](https://mas.owasp.org/MASVS/), worked through before release.

## Reporting a problem

If you find a security issue, don't open a public issue. Contact the maintainer directly.
