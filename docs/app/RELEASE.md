# Releasing to Google Play

How a build gets from `main` to Google Play, and the one-time setup before the first one. Steps marked **(Bruce)** need the Play account or the upload key, so they can't be done from the repo.

## How a release is shaped

- **One Play app, two bundles.** The phone and watch apps share the application id `app.levitx`, so they are one listing with Wear OS added as a form factor. Each upload is an Android App Bundle (`.aab`).
- **Version codes.** `mobile/gradle.properties` holds `levitx.versionName` (what people see) and `levitx.release` (goes up by one for every upload). The phone's version code is the release times ten, and the watch's is one more: release 7 is 70 and 71. Play needs every bundle in an app to have its own code, the watch included.
- **Signing.** Play App Signing: Google keeps the key that signs what people install. We sign uploads with an upload key, which Google can replace if it's ever lost or leaked. The upload key lives in a password manager and in one GitHub environment, nowhere else.
- **Tracks.** Internal testing first, then a closed test, then production.

## One-time setup

### 1. Play Console account (Bruce)

Create a Google Play developer account. It has a one-off registration fee. Decide whether it is a personal account or an organisation one first. A **personal account created after 13 November 2023 must run a closed test with at least 12 testers, opted in for 14 days in a row, before it can apply for production.** An organisation account needs a D-U-N-S number but skips that.

### 2. The upload key (Bruce)

Make it on your own computer, not on a shared or CI machine:

```sh
keytool -genkeypair -keystore levitx-upload.p12 -storetype PKCS12 \
  -alias levitx-upload -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Levitx upload key"
```

Use a long random password and put both the file and the password in your password manager. The alias must be `levitx-upload`; the build looks for it.

Then, in GitHub, under **Settings → Environments**, create an environment called `release`:

- **Deployment branches:** `main` only.
- **Required reviewers:** yourself, so every release build waits for your approval.
- **Secrets:**
  - `LEVITX_UPLOAD_KEYSTORE_BASE64`: the output of `base64 -w0 levitx-upload.p12` (`base64 -i levitx-upload.p12` on a Mac).
  - `LEVITX_UPLOAD_KEYSTORE_PASSWORD`: the password.

Delete any copy of the base64 text afterwards. The keystore file stays only in the password manager.

### 3. Create the app (Bruce)

In Play Console, create the app, accept Play App Signing (the default), and register the upload key when the first bundle is uploaded. If Play asks for the upload certificate, export it with:

```sh
keytool -export -rfc -keystore levitx-upload.p12 -alias levitx-upload -file upload-certificate.pem
```

The SHA-256 Play shows for the upload key should match the one in the release workflow's summary.

Then add Wear OS under **Test and release → Advanced settings → Form factors**, with its screenshots.

## Each release

1. Raise `levitx.release` by one in `mobile/gradle.properties`, and `levitx.versionName` if it's more than a fix. Merge to `main`.
2. In GitHub, run **Actions → Mobile release → Run workflow** on `main`, and approve it.
3. Download `levitx-release-<version>` from the run's page. It has both bundles and R8's two maps. Play reads the map from the bundle itself; the copies are there in case.
4. In Play Console, upload the phone bundle to the track, and the watch bundle to the same track's Wear OS release. Paste release notes, then review and roll out.

The first time, upload to **Internal testing** and try it on your own phone and watch before anything else. Then go through the checklist in [TESTING-ON-DEVICES.md](TESTING-ON-DEVICES.md), with the release build this time.

## App content forms

Play Console's **Policy → App content** page needs these before the first release. Draft answers, to check before submitting:

- **Privacy policy:** `https://levitx.app/privacy`. It covers the app and the website. Keep it in step with the apps (see the note at the top of `src/app/privacy/page.tsx`).
- **Ads:** no ads.
- **App access:** everything works without signing in.
- **Data safety:** see below.
- **Foreground services:** both apps use the `health` type, the phone for a workout running with the phone locked ([ADR 0008](adr/0008-phone-workout-off-screen.md)) and the watch for a session with the screen off ([ADR 0006](adr/0006-watch-workout-service.md)). Play asks for a short video of each. Record a workout started, the phone locked, the rest countdown on the lock screen and the beep at the end; and the same on the watch with the wrist down.
- **Health:** if Play asks for a health apps declaration, Levitx is fitness and exercise tracking. It reads heart rate on the watch during a session (shown, not stored) and doesn't use Health Connect yet.
- **Content rating:** fill in the questionnaire. Nothing in the app should raise the rating.
- **Target audience:** your call. If any age group under 18 is included, more policies apply, so decide on purpose.

### Data safety, draft

**Does the app collect or share any of the required user data types? No.** The reasoning, for checking:

- Play counts data as collected only when it's sent off the device. Data the app only uses on the device isn't collected. Everything Levitx stores stays in the encrypted database on the phone (and the watch).
- The phone and watch talk through Google Play services' Wearable Data Layer: Bluetooth, or Google's servers with end-to-end encryption when Bluetooth isn't available. That's the person's own devices, end-to-end encrypted, and never readable by us or Google. We're treating it as not collected. This is the answer most worth a second look.
- The export goes wherever the person chooses to save it, as an action they take. That isn't the app sharing data.
- No analytics, crash reporting, ads or other SDKs that send data anywhere. If one is ever added, this answer changes.

## Store listing draft

Written to match what the app does today. Change anything that stops being true.

- **Name:** Levitx
- **Short description (80 characters at most):** Calisthenics that plans your next session, on your phone and Wear OS watch.
- **Full description:**

  > Levitx is a calisthenics coach that plans every session from what you actually did last time.
  >
  > Pick a goal, like your first pull-up or a muscle-up, and answer a few questions about where you're starting. Levitx plans today's session: the exercises, the sets and how many reps to aim for, with rest timed between them. Log each set and how hard it felt, and the next session moves with you. Harder when it was easy, easier when it wasn't.
  >
  > Skill work comes in gates. Pass one and the next opens. Holds and slow negatives have their own timers, paced with beeps, so the phone can sit on the floor while you work. Rest counts down on your lock screen.
  >
  > Train from your Wear OS watch too. Sessions run on the watch with the phone in your bag, and they come back to the phone when you're done.
  >
  > Your training stays on your phone. No account, no ads and no analytics. Everything is encrypted on the device, and you can export it or delete it any time.

- **Graphics:** the 512 × 512 icon, a 1024 × 500 feature graphic, phone screenshots (Today, a set, rest, Progress, Skills), and Wear OS screenshots. The phone screens can come from the CI preview renders, re-shot on a device for the final listing.

## Before production

- The closed test, if the account needs it (see step 1).
- The device checks in [TESTING-ON-DEVICES.md](TESTING-ON-DEVICES.md), on the release build, on a phone and a watch.
- The open items in [MASVS-REVIEW.md](MASVS-REVIEW.md).
- The watch Tile's preview image for the tile picker, from a screenshot of the real Tile (see the note in the watch manifest).
