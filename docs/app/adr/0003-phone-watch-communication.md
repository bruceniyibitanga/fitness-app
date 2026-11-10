# 0003: Phone and watch talk over the Wearable Data Layer

**Status:** Accepted, November 2026. Revised twice the same month: once when the stack moved to Kotlin ([0005](0005-kotlin-compose-and-wear-compose.md)), and again when sync was built and sessions moved from messages to data items (see "What changed").

## Context

The watch has to run sessions without the phone, then hand the results back. Android's [Data Layer docs](https://developer.android.com/training/wearables/data/overview) say it is the API to use for phone and watch communication, and set the requirements: the package name and signing key must match on both devices, and traffic may pass through Google servers.

The Data Layer offers two tools that matter here ([Choose a client type](https://developer.android.com/training/wearables/data/client-types)):

- **Data items** are stored by Google Play services and delivered whenever the devices are next connected. Reading and writing both work offline.
- **Messages** only work while the devices are connected, and have no retry.

## Decision

- **Google's library, called directly.** `play-services-wearable`, wrapped in one small class (`PlayServicesDataLayer` in `:sync`). Nothing else touches the API.
- **Everything is a data item.** The whole point is working with the phone out of reach, which is what data items are for.
- **Phone to watch: one item, `/levitx/state`,** holding the goal, the best per gate and the bands. The watch copies it into its own database, so it plans and runs sessions offline.
- **Watch to phone: one item per finished session, `/levitx/sessions/<id>`.** The handshake:
  1. The watch saves the session and queues it, in one database transaction. Then it writes the data item.
  2. The phone stores the session, then writes a confirmation item, `/levitx/acks/<id>`.
  3. The watch sees the confirmation, takes the session off its queue and deletes its session item.
  4. The phone sees the session item is gone and deletes its confirmation.

  Each device only ever deletes its own items, and nothing is left behind.
- **Sync works from the current items, not from events.** A pass lists what is on the Data Layer and brings both sides into line. Events only say "run a pass". A missed, repeated or out-of-order event can't break anything.
- **Nothing can be lost or counted twice,** because of how each kind of data behaves:
  - The goal belongs to the phone. The watch only copies it.
  - A best only ever goes up. Applying it late or twice changes nothing.
  - A session has a random UUID made when it starts, and each device stores an id once.
- **Payloads are versioned and validated on arrival.** They are small JSON documents with a version number. Size, version and every value are checked before anything reaches the database. Anything invalid is skipped whole. Anything from a newer app version is left alone until this app is updated.
- **What crosses the link is the minimum** a session needs. Bodyweight, pain notes, physio documents and photos never go to the watch. The phone adds its own bodyweight record to a session when it arrives.
- **Each app has a manifest-registered `WearableListenerService`,** so Play services can wake it when data arrives, filtered to our own paths.
- **Workout sensors on the watch** come from Health Services `ExerciseClient`, run inside a foreground service of type `health` so the workout keeps going with the screen off.

## What changed

The first version of this ADR sent sessions back as messages held in an outbox, and planned to use Horologist's Data Layer helpers.

- **Messages became data items.** Messages need a live connection and have no retry, so we would have had to build our own retry loop and wake-ups. Data items already do that: Play services keeps them and delivers them later. The outbox stays, as a database table, because the Data Layer docs say it is for syncing and not a place to keep data.
- **No Horologist.** The wrapper we need is about 60 lines. A dependency wasn't worth it.

## Consequences

- We depend only on official libraries, and we control all the code that touches health data.
- Retries can't double-count sets, because session ids are unique and stored once.
- The sync rules are plain Kotlin behind an interface, so they are tested on the JVM with a pretend phone and watch. What those tests can't cover is the real Data Layer between a real phone and watch. That needs a manual check on devices.
- The watch app is marked non-standalone, because the goal is chosen on the phone.
- Both apps must always be signed with the same key. Play App Signing handles this for release builds; debug builds on one machine already share a key.
