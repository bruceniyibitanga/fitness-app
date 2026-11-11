# Testing on a real phone and watch

You don't need Android Studio for this. Every push to `main` that touches the apps builds them in GitHub Actions, and you install the results on your phone and watch.

## What you need

- An Android phone and a Wear OS watch, already paired with each other in the usual way (the Pixel Watch or Galaxy Wearable app, for example).
- To install on the watch: a computer on the same Wi-Fi as the watch, with Google's `adb` tool. It comes in [SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools), a small download. Unzip it anywhere; there's nothing to install.

## One-time setup: the test signing key

The phone and watch apps only talk to each other when both are signed with the same key. CI signs every test build with a shared test key kept in the repository secrets, so builds always match and each new one installs over the last without wiping your data.

1. On GitHub, open the repo's **Settings > Secrets and variables > Actions**.
2. Add two repository secrets, pasting each file's contents exactly:
   - `LEVITX_TEST_KEYSTORE_BASE64`
   - `LEVITX_TEST_KEYSTORE_PASSWORD`
3. Delete both files from your computer once they're saved. GitHub keeps the only copy it needs.

The key's SHA-256 fingerprint is:

```
43c46c8aa133f37ccae2097f8638dd54f4cfd288bfb1271cb8d486c69324e4e7
```

Each test build's summary page shows the fingerprint it was signed with, so you can check it matches.

This key is for test builds only. Release builds will be signed through Play App Signing, with a different key. If this one is ever lost or leaked, make a new one and replace the two secrets. The only cost is uninstalling the old test builds once.

Without the secrets, builds still work, but each one gets a throwaway key, so you'd have to uninstall before every new build. The run shows a warning when that happens.

## Getting a build

1. Open the repo's **Actions** tab and pick the latest **Mobile test build** run. To build without pushing anything, open the workflow and use **Run workflow**.
2. At the bottom of the run's page, under **Artifacts**, are `levitx-phone-test-<number>.apk` and `levitx-watch-test-<number>.apk`. They download as plain APK files. You need to be signed in to GitHub, and they're kept for 30 days.

The number is also in the app's version name (for example `0.1.0-test.12`), shown under the app in your device's Settings > Apps, so you can tell which build is installed.

## Installing on the phone

1. On the phone, open the run's page in your browser and download `levitx-phone-test-<number>.apk`.
2. Open the download. The first time, Android asks you to allow installs from your browser: allow it, then go back and tap **Install**.

Later builds install the same way and keep your data.

## Installing on the watch

The watch has no browser, so the APK goes over Wi-Fi from your computer.

**Once per watch: turn on developer options.**

1. On the watch, open **Settings > System > About** (on Galaxy watches, **Settings > About watch > Software information**).
2. Tap **Build number** (on Galaxy watches, **Software version**) seven times, until it says you're a developer.
3. In the new **Developer options**, turn on **ADB debugging** and **Wireless debugging**. The watch must be on Wi-Fi, the same network as your computer.

**Once per computer: pair.**

1. On the watch, in **Wireless debugging**, tap **Pair new device**. It shows a pairing code and an address like `192.168.1.40:37017`.
2. On your computer, in the folder where you unzipped Platform-Tools, run:
   ```
   adb pair 192.168.1.40:37017
   ```
   and type the pairing code.

**Each time you install.**

1. Back on the watch's **Wireless debugging** screen, note the address under **IP address & port**. It's a different port from the pairing one.
2. Run:
   ```
   adb connect 192.168.1.40:41209
   adb install -r levitx-watch-test-<number>.apk
   ```
   `-r` replaces the installed version and keeps its data.

The watch app appears in the app list within a few seconds. The connection drops when the watch sleeps or changes networks, so run `adb connect` again next time.

Turn wireless debugging off when you're not installing, since it uses battery.

## If something goes wrong

- **"App not installed" on the phone, or `INSTALL_FAILED_UPDATE_INCOMPATIBLE` from adb:** the installed copy was signed with a different key, usually a build from before the secrets were added. Uninstall it, then install again. This wipes that device's Levitx data.
- **The watch never shows your goal:** check both builds have the same fingerprint on their run pages, and that the phone and watch are paired. Then follow the sync checks in the Levitx device test checklist.
- **`adb` can't find the watch:** make sure both are on the same Wi-Fi, wireless debugging is still on, and you're using the address from the main Wireless debugging screen, not the pairing one.

## What these builds are

They are debug builds: the same code as a release, but without R8 shrinking, and debuggable so log lines can be read with `adb logcat`. Speed and battery use are a little worse than a release build, so treat battery numbers as a rough upper bound.
