# 0003: Phone and watch talk over the Wearable Data Layer

**Status:** Accepted, November 2026. Revised the same month when the stack moved to Kotlin ([0005](0005-kotlin-compose-and-wear-compose.md)); the Flutter bridges this ADR first described are no longer needed.

## Context

The watch has to run sessions without the phone, then hand the results back. Android's [Data Layer docs](https://developer.android.com/training/wearables/data/overview) say it is the API to use for phone and watch communication, and set the requirements: the package name and signing key must match on both devices, and traffic may pass through Google servers.

## Decision

- **Google's libraries, called directly.** `play-services-wearable` for the Data Layer, with [Horologist's Data Layer helpers](https://google.github.io/horologist/) where they save code, and coroutine-based APIs throughout.
- **The plan goes to the watch as a data item.** Data items persist and are delivered when the watch is next connected.
- **Finished sets come back as messages.** Each has a unique ID and sits in a local outbox until the phone acknowledges it.
- **Payloads are versioned and validated on arrival.** Anything unexpected is dropped and logged without its contents.
- **What crosses the link is the minimum** a session needs: the plan, and the sets that come back. Pain notes, physio documents and photos never go to the watch.
- **Workout sensors on the watch** come from Health Services `ExerciseClient`, run inside a foreground service of type `health` so the workout keeps going with the screen off.

## Consequences

- We depend only on official libraries, and we control all the code that touches health data.
- Retries can't double-count sets, because set IDs are unique.
- Both apps must always be signed with the same key. Play App Signing handles this for release builds; debug builds on one machine already share a key.
