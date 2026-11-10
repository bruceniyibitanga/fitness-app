# 0006: The watch workout runs in a foreground service, and the rest alert uses a short wake lock

**Status:** Accepted, November 2026. The rest alert decision needs checking on a real watch before release.

## Context

A session on the watch lasts ten minutes or more. For most of it the person isn't looking at the screen: they are hanging from a bar or resting with their wrist down. Three things have to keep working anyway:

1. The session must not be lost when the watch dims or the person swipes to the watch face.
2. Heart rate should be there when they glance back.
3. The buzz at the end of a rest must arrive on time. A rest alert that is 40 seconds late is worse than none.

The [Wear OS workout guide](https://developer.android.com/health-and-fitness/health-services/active-data) covers the first two: run the workout in a foreground service, read sensors through Health Services `ExerciseClient`, and show an ongoing activity. From Wear OS 5 the service must have the `health` type. The [app quality guidelines](https://developer.android.com/docs/quality-guidelines/wear-app-quality) (WO-V4) require an ongoing activity for anything long-running.

The third has no ready answer. With the screen off, a watch puts its processor to sleep and an ordinary timer fires late.

## Decision

### The service

- **`WorkoutService` is a foreground service of type `health`.** It starts when a session is on screen and stops itself when no session is running.
- **It doesn't hold the session.** `WorkoutController` does (see ARCHITECTURE.md). The service watches the controller. So a session works the same with or without the service, and nothing is lost if the service can't start.
- **`ExerciseTracker` records a Health Services exercise** (calisthenics, or the closest type the watch supports) for the length of the session, and reads heart rate from it. It never takes over another app's workout.
- **`WorkoutNotifications` pairs the service notification with an `OngoingActivity`,** so the workout shows on the watch face and in the launcher.
- **Heart rate is shown and nothing else.** It isn't written to the database and isn't synced to the phone. If we later want it in session history, that is a new decision with its own privacy review.

### Permissions

- **Heart rate and notifications are asked for once, when the first session starts,** on a screen that says what they are for.
- **"Not now" is a real choice.** The session runs without them: no heart rate and no service, and the screen stays on so the timers still work.
- **Wear OS 6 (API 36) split heart rate out of `BODY_SENSORS`.** Both permissions are declared, each for the versions that use it.

### The rest alert

**While resting, the service holds a partial wake lock for the length of the rest, then vibrates.**

- It is held only during rest: two minutes at most, three times a session.
- It always has a time limit, so the system releases it even if our code never does.
- It is released at once when the person skips the rest.

## Alternatives considered for the rest alert

- **An exact alarm (`AlarmManager.setExactAndAllowWhileIdle`).** This is the usual tool for a timer. From Android 14, which Wear OS 5 is built on, a newly installed app doesn't have the exact alarm permission: the person has to find a setting and switch it on. Asking for that on a watch, for a buzz, is a poor trade. The other route, `USE_EXACT_ALARM`, is reserved by Google Play policy for alarm clock, timer and calendar apps.
- **A Health Services duration goal.** `ExerciseClient` can wake the app when an exercise reaches a set duration. It is the most battery-friendly option on paper, but which watches support it for this exercise type, and how promptly it is delivered with the screen off, isn't documented. It couldn't be tested without a watch. Worth revisiting once one is available.
- **Only buzzing while the app is on screen.** Simple, but it fails exactly when the alert matters: wrist down, screen off.

## Consequences

- **This goes against the general advice to avoid wake locks.** The [power guide](https://developer.android.com/training/wearables/apps/power) says "in most cases". We think a timer that must fire on time with the screen off, for at most two minutes, is one of the other cases. The cost is the processor staying awake for about six minutes per session.
- **None of this could be run on a real watch.** It compiles, passes lint, and the logic around it is unit tested with a fake. The service, Health Services and the wake lock itself need a session on a real watch, with the wrist down during rest, before release. If the alert is late or the battery cost is noticeable, the duration goal is the first thing to try.
- **The screen still stays on while the session screen is showing.** The hold timer lives on that screen and has the same sleeping-processor problem. Letting the screen dim during a session is a later improvement.
