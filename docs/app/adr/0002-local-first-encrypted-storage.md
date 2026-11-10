# 0002: Local-first, encrypted storage

**Status:** Accepted, November 2026. The backup detail is still open, see below.

## Context

The app stores health information. A server would cost money and would make us the custodian of everyone's health data, with the legal and breach risk that brings. The maintainer chose "on the device, backed up to the user's own cloud" over running a backend.

## Decision

- **Database.** All data lives in a local SQLite database through [drift](https://drift.simonbinder.eu/), encrypted at rest. Drift's [encryption docs](https://drift.simonbinder.eu/platforms/encryption) changed recently: from drift 2.32 they use SQLite3MultipleCiphers. We follow whatever those docs say when we build it in Phase 1, rather than an older guide.
- **Key.** The database key is random, made on the device, and held by Android Keystore-backed secure storage.
- **Backups.** Only allowed when Android's client-side encryption is on (Android 9 or later, with a screen lock).

## Consequences

- **No server to run, pay for or secure.** Users own their data.
- **No sync between a user's devices** until we add it on purpose.
- **Open: restoring a backup on a new phone.** Keystore keys don't leave the device, so a restored database would need its key restored too. We'll pick between excluding the database from backup (and offering an encrypted export) and wrapping the key, in a follow-up ADR before real data is stored.
