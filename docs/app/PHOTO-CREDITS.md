# Photo credits

The photos in the phone app come from [Unsplash](https://unsplash.com) and are used under the [Unsplash License](https://unsplash.com/license), which allows use in apps without asking or paying. Credit isn't required, but we give it.

Each photo was cropped, resized and saved as WebP with its metadata (camera, location and the like) removed. They live in `mobile/app-phone/src/main/res/drawable-nodpi/`, and `ui/Photos.kt` decides where each one is used.

| File | Used for | Photo |
|---|---|---|
| `photo_welcome.webp` | The welcome screen | [Shoham Avisrur](https://unsplash.com/photos/3HnxyD3J1Qg) |
| `photo_goal_first_pull_up.webp` | The first pull-up program, pull workouts and pulling exercises | [Anastase Maragos](https://unsplash.com/photos/IZw5zLHMfd4) |
| `photo_goal_muscle_up.webp` | The muscle-up program | [Kate Trysh](https://unsplash.com/photos/A2OmNsPhVj4) |
| `photo_focus_push.webp` | Push workouts and pushing exercises | [Unsplash photo qlid9Lys8r0](https://unsplash.com/photos/qlid9Lys8r0) |
| `photo_focus_upper.webp` | Upper body workouts | [Unsplash photo fG0p4Qh_aWI](https://unsplash.com/photos/fG0p4Qh_aWI) |
| `photo_focus_legs.webp` | Leg workouts and leg exercises | [Unsplash photo 928VXYtfsRI](https://unsplash.com/photos/928VXYtfsRI) |
| `photo_focus_core.webp` | Core workouts and core exercises | [Unsplash photo Pu7BOaYlYGY](https://unsplash.com/photos/Pu7BOaYlYGY) |
| `photo_focus_full_body.webp` | Full body workouts | [Unsplash photo QwAJTPXbj88](https://unsplash.com/photos/QwAJTPXbj88) |

Each link opens the photo's page on Unsplash, which names the photographer.

Adding a photo: download it through Unsplash (so the download is counted for the photographer), crop it, strip its metadata, save it as WebP here, add it to `Photos.kt`, and add a row to this table.
