# OWASP MASVS review

A pass over the phone and watch apps against the [OWASP Mobile Application Security Verification Standard](https://mas.owasp.org/MASVS/) (version 2), before the first release. Each control says how the apps meet it, with where to look, or why it doesn't apply. Open items are at the end.

Done in November 2026 by reading the code and manifests. Nothing here was run against a release build on a device yet; the last section says what needs that.

## Why the scope is what it is

Levitx keeps training and bodyweight data, which is health data, so storage and privacy get the most weight. It has no account, no server, no payments and no secrets of its own to protect, and its data is only worth anything to the person it belongs to. So the resilience controls (anti-tampering, root detection) are out of scope on purpose: they'd add cost and false alarms without protecting anyone.

## Storage

**MASVS-STORAGE-1: sensitive data is stored securely. Met.**
- Everything the person logs is in one SQLCipher database (`:data`). Its 256-bit key is random, encrypted with an AES-256-GCM key in the Android Keystore that can't be exported (StrongBox where the device has it), and stored in `noBackupFilesDir`. See `DatabaseKeyStore.kt` and [ADR 0002](adr/0002-local-first-encrypted-storage.md).
- Settings live in a DataStore file outside the database because none of them is personal (the count-in, sounds, whether notifications were asked for).
- The export is a plain zip, saved only where the person picks with Android's save dialog. Settings and the privacy policy say it isn't encrypted.

**MASVS-STORAGE-2: sensitive data doesn't leak. Met, with two choices noted.**
- Backup is off, and the data extraction rules exclude every domain from cloud backup and device-to-device transfer, in both apps.
- Logs carry codes and counts, never values (`SyncReport.kt`, the watch's service logs). There are no other log calls.
- No clipboard use, no WebViews, no files in shared storage except the export the person saves.
- **Choice:** the workout notifications are public, so they show on a locked phone or watch. They show the exercise, the set and the clocks, never reps, loads or weight ([ADR 0008](adr/0008-phone-workout-off-screen.md)).
- **Choice:** screens aren't marked secure, so they show in the recent apps list and can be screenshotted. People share their training on purpose; blocking screenshots would get in their way more than it protects them. Revisit if the app ever shows something more sensitive, like pain notes.

## Cryptography

**MASVS-CRYPTO-1: strong cryptography, used properly. Met.**
- AES-256-GCM with a 128-bit tag and a fresh random 12-byte IV for the key file. SQLCipher's own defaults for the database. No custom crypto, no hard-coded keys, no deprecated algorithms.

**MASVS-CRYPTO-2: keys are managed properly. Met.**
- The wrapping key is created in and never leaves the Keystore. The database key exists in memory only to open the database. Deleting everything clears the tables; the keys stay, which is fine because they protect nothing once the data is gone.
- The release upload key is outside the repo and outside every build except the release workflow, and Play App Signing can replace it ([RELEASE.md](RELEASE.md)).

## Authentication

**MASVS-AUTH-1 to 3: not applicable.** There are no accounts, logins or server sessions. The phone's lock screen protects the data, and the database key is only usable on the device. An optional app lock (fingerprint or PIN) could come later if people ask for one.

## Network

**MASVS-NETWORK-1 and 2: met by not having a network.** Neither app asks for the internet permission, and CI fails the build if any permission appears that isn't on its list (`mobile/config/permissions`). Phone and watch talk only through Google Play services' Wearable Data Layer: an encrypted Bluetooth channel, or Google's servers with end-to-end encryption when Bluetooth isn't available. Only the same app, signed with the same key, can read what the other side sends.

## Platform

**MASVS-PLATFORM-1: IPC is secure. Met.**
- Exported components are only the ones Android needs: the phone's and watch's launcher activities, the Data Layer listeners (which only Google Play services can call, and only with data from our own signed app), and the watch Tile service (bound only by the system). The phone's workout service and the watch's are not exported.
- All `PendingIntent`s are immutable and explicit.
- Everything that arrives over the Data Layer is size-limited and validated field by field before use, and unreadable payloads are dropped (`SyncCodec.kt`, with tests that send bad input on purpose).

**MASVS-PLATFORM-2: WebViews are secure. Not applicable.** There are none.

**MASVS-PLATFORM-3: the UI is secure. Met.** Deleting a workout or everything asks first on a separate sheet. Android 12 and later block touches through untrusted overlays by default, which covers tapjacking on the delete confirmations for nearly every phone that runs Levitx.

## Code

**MASVS-CODE-1: requires an up-to-date platform. Met, with one note.** Both apps target the newest Android (API 37). The phone supports back to Android 9 (API 28), which no longer gets security patches. That widens who can use the app, and the data is still encrypted with Keystore keys there. Raise it when the share of Android 9 phones drops.

**MASVS-CODE-2: a way to make people update. Not needed now.** With no server, there is no old client to shut out. If a release ever fixes a serious flaw, Play's in-app updates can make it required.

**MASVS-CODE-3: no components with known vulnerabilities. Partly met.** Every dependency is pinned, Dependabot opens weekly update PRs, repositories are locked down so a look-alike package can't be pulled, and the Gradle wrapper is checksummed. What's missing is an alert when a pinned version gets a published vulnerability between updates. See open items.

**MASVS-CODE-4: untrusted input is validated. Met.** The only untrusted input is what arrives over the Data Layer, validated as above. Form fields are numbers with limits, or band names with a length limit. The app doesn't import files yet; when the planned import arrives, it needs the same treatment as sync.

**Release builds:** R8 shrinks and obfuscates both apps, debug flags are off, and CI builds the release variant on every push.

## Resilience

**MASVS-RESILIENCE-1 to 4: out of scope,** for the reasons at the top.

## Privacy

**MASVS-PRIVACY-1: access to sensitive data is minimised. Met.** Permissions are the smallest set the features need, each explained in the manifest and [SECURITY.md](SECURITY.md), and pinned by CI. Bodyweight never goes to the watch. Heart rate is shown on the watch and never stored or sent. The one permission held without a direct use, `HIGH_SAMPLING_RATE_SENSORS`, is there because Android requires a sensor permission for the `health` service type, and it's the only one that doesn't prompt; the app reads no sensors.

**MASVS-PRIVACY-2: the person isn't identified. Met.** No account, no device or advertising ids, no analytics, no crash reporting.

**MASVS-PRIVACY-3: data use is transparent. Met.** The privacy policy at levitx.app/privacy covers the app, and the Data safety answers are drafted in [RELEASE.md](RELEASE.md). Notifications are asked for in context, once.

**MASVS-PRIVACY-4: the person controls their data. Met.** They can export everything, delete one workout, or delete everything, from Settings and History. Deleting everything on the phone deletes the watch's copy too, the next time the watch is connected, even if the person has set up again by then (each delete has an id the watch acts on once). Uninstalling the phone app can't reach the watch, which the privacy policy says.

## Open items

1. **Turn on Dependabot alerts** (Settings → Code security), so a published vulnerability in a pinned dependency raises an alert without waiting for the next update PR. (Bruce, a repository setting.)
2. **Password-protected export,** planned with the import for moving to a new phone. The import must validate its input like sync does.
3. **On a release build on devices:** check the database can't be read with `adb` (it should be encrypted bytes), that nothing personal appears in `adb logcat` during a workout and a sync, and the lock screen content on both devices, and that deleting everything on the phone clears the watch.
