# 0002: Local-first, encrypted storage, no automatic backup

**Status:** Accepted, November 2026. Revised the same month when the stack moved to Kotlin ([0005](0005-kotlin-compose-and-wear-compose.md)) and the backup question was settled.

## Context

The app stores health information. A server would cost money and would make us the custodian of everyone's health data, with the legal and breach risk that brings. The maintainer chose to keep data on the device.

That left one open question: backups. Android Auto Backup can copy app data to the user's Google Drive, but the database is encrypted with a key held in the Android Keystore, and Keystore keys never leave the device. A backed-up database restored on a new phone would be unreadable.

## Decision

- **Database.** All data lives in a local SQLite database through [Room](https://developer.android.com/training/data-storage/room), Google's recommended database library, encrypted at rest with SQLCipher. We confirm the exact integration against the current Room and SQLCipher docs when we build it in Phase 1.
- **Key.** The database passphrase is 256 random bits made on the device the first time the app runs. It is stored only after being encrypted with an AES-GCM key that lives in the Android Keystore.
- **No automatic backup or device transfer.** Both apps set `allowBackup="false"` and ship data extraction rules that exclude every domain from cloud backup and device-to-device transfer. `allowBackup` alone doesn't stop device transfer on Android 12 and later, which is why the rules are needed too.
- **Password-protected export instead.** People move their data to a new phone with an export file encrypted with a password they choose. The maintainer approved this in November 2026. It is built in Phase 4.

## Consequences

- **No server to run, pay for or secure.** Users own their data.
- **Nothing leaves the device unless the person exports it.** No copy of their health data sits in a cloud account they may have forgotten about.
- **Losing the phone without an export loses the data.** The app will remind people to export now and then, gently, once they have a meaningful history.
- **No sync between a user's devices** until we add it on purpose, with its own ADR.
