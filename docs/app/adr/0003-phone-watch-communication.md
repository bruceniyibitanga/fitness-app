# 0003: Phone and watch talk over the Wearable Data Layer via Pigeon bridges

**Status:** Accepted, November 2026

## Context

The watch has to run sessions without the phone, then hand results back. Android's [Data Layer docs](https://developer.android.com/training/wearables/data/overview) say it is the only API to use for phone to watch communication. They also set the requirements: the package name and signing key must match on both devices, and traffic may pass through Google servers. Community Flutter plugins exist for the Data Layer, but they're small projects with uneven maintenance.

## Decision

- **Our own bridge.** We write a thin Kotlin bridge over the official `play-services-wearable` library and generate the Dart side with [Pigeon](https://docs.flutter.dev/interop/pigeon).
- **Plan goes to the watch** as a data item. It's persistent and delivered when the watch is next connected.
- **Finished sets come back** as messages. Each has a unique ID and sits in a local outbox until the phone acknowledges it.
- **Payloads** are versioned and validated on arrival. No health notes or documents are sent.
- **Workout sensors.** The watch uses Health Services `ExerciseClient` for heart rate and the ongoing workout, through the same kind of Pigeon bridge.

## Consequences

- We depend only on Google's official libraries, and we control the code that touches health data.
- We maintain about two small Kotlin files per bridge. Pigeon removes the error-prone part: channel names and type conversion.
- Retries can't double-count sets, because set IDs are unique.
