# 0008: A phone workout keeps going off screen, in a health foreground service with a Live Update

**Status:** Accepted, November 2026. Needs checking on a real phone before release (see Consequences).

## Context

Until now the phone workout only worked while it was on screen. The screen stayed on, and the beeps were played by the screen itself. That broke in the most ordinary moments of a workout:

- The person locks the phone between sets, or it goes in a pocket. The beep at the end of rest never comes.
- They switch to their music or a message during rest. Android stops drawing the app, so nothing plays, and a few seconds later it may pause the app altogether.
- There is nothing on the lock screen to say how long is left.

Three things are needed: something that keeps the app running when it isn't on screen, a way to play a beep on time with the processor asleep, and a way to show the rest clock outside the app.

## Decision

### One cue player, not the screen

- **`WorkoutCuer` plays every beep and buzz,** whether the screen is on or not. It follows `LiveWorkout`, a read-only picture of the workout that the workout screen's view model publishes after every change. The workout itself stays with the view model and the saved draft, as before.
- **It sleeps until the next second that has a cue.** Every timer changes on whole seconds counted from one moment (rest's end, a hold's start), so the rules for each cue (`CueTracks.kt`) can say exactly when the next one is due. Rest wakes it once, three seconds before the end.
- **While a cue is due, it holds a partial wake lock,** with a time limit each time, released as soon as the rest or timer ends or changes. This is the same trade the watch makes ([ADR 0006](0006-watch-workout-service.md)), for the same reasons.
- **The screen plays nothing,** so a beep can never be played twice. Buzzes now go through the phone's vibrator with the system's own tick and click effects, rather than through the screen's touch feedback.

### A foreground service of type `health`

- **`LiveWorkoutService` keeps the app running** while a workout is in progress. The workout screen starts it, while it is in front, which is when Android allows it. It stops itself as soon as `LiveWorkout` is empty.
- **The type is `health`.** Android's [service types guide](https://developer.android.com/develop/background-work/services/fgs/service-types) describes it as being for "apps in the fitness category such as exercise trackers", which is what this is.
- **From Android 14 a `health` service needs one sensor permission.** The guide lists the ones that qualify. All of them are prompts except `HIGH_SAMPLING_RATE_SENSORS`, which is granted at install, so that is the one declared. Levitx reads no sensors. The manifest says so next to it.
- **If the service can't start,** the workout still runs and beeps while it's on screen. Nothing depends on it.

### A Live Update

- **The service's notification asks to be a Live Update** (`setRequestPromotedOngoing`). On Android 16 and later it sits on the lock screen and as a status bar chip with the rest countdown. On earlier versions it is an ordinary ongoing notification. Android's [Live Update guidance](https://developer.android.com/develop/ui/views/notifications/live-update) lists starting a workout as a fitting use.
- **It shows the exercise, the set, a progress bar for the workout (one segment per exercise) and the rest or hold clock.** Android draws the clock from one start or end time, so the notification is only posted again when something changes.
- **It is public, so it shows on a locked phone.** It never shows reps, loads, bodyweight or anything else from the log.
- **It is silent.** The channel has no sound or vibration, and updates never alert. The cue player does the beeping.
- **If the person swipes it away, it stays away** until the next workout, as the guidance asks.

### Asking for notifications

- **Android 13 and later ask the person before an app can show notifications.** Levitx asks once, the first time a workout opens, which is when the reason is obvious. Either answer is fine: without notifications the service still runs and the beeps still come, with nothing on the lock screen.
- **It doesn't ask again.** Settings has a Lock screen row that shows whether notifications are on and opens Android's settings for Levitx.

## Alternatives considered

- **An exact alarm for the end of rest.** From Android 14 a new install doesn't have the exact alarm permission, and the person has to find a setting to turn it on. `USE_EXACT_ALARM` avoids that, but Google Play reserves it for alarm clock, timer and calendar apps. An alarm also only covers rest, not the count-in and pacing beeps of holds and negatives.
- **The `specialUse` service type.** It needs a written case reviewed by Play, for a use the `health` type already describes.
- **A `shortService`.** It has a limit of about three minutes, and a workout is much longer.
- **A Live Update with no service.** The countdown would show, drawn by Android, but nothing would keep the app running to beep at the end.
- **Asking for `ACTIVITY_RECOGNITION` to qualify for the `health` type.** That is a prompt for a sensor Levitx doesn't use, which is worse for the person than a permission granted quietly at install for the same purpose.

## Consequences

- **The processor stays awake during rest,** a few minutes per workout. Google Play's battery measure counts wake locks held in the background or by a foreground service, and calls it excessive at two hours in a day. A long workout with the screen off all the way through is well under that.
- **The Play Console needs the foreground service declared** (Policy, App content) with a short video of the workout running with the phone locked. That is part of release prep.
- **None of the service, the wake lock or the Live Update could be run here.** The cue timing is unit tested on a test clock, and everything compiles and passes lint. On a real phone, before release, check: the rest beep with the phone locked and in a pocket for a few minutes; the beep with another app in front; the lock screen and status bar on Android 16; the notification on an older phone; denying notifications; and the battery cost of a full workout.
