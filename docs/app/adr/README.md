# Architecture decision records

One short file per decision that would be expensive to change later. Each says what we decided, why, and what we gave up.

| # | Decision | Status |
|---|---|---|
| [0001](0001-flutter-for-android-and-wear-os.md) | Flutter for the Android phone and Wear OS apps | Superseded by 0005 |
| [0002](0002-local-first-encrypted-storage.md) | Local-first, encrypted storage, no automatic backup | Accepted |
| [0003](0003-phone-watch-communication.md) | Phone and watch talk over the Wearable Data Layer | Accepted |
| [0004](0004-mvvm-and-shared-core.md) | Android's recommended architecture, plus a pure Kotlin core | Accepted |
| [0005](0005-kotlin-compose-and-wear-compose.md) | Kotlin, Jetpack Compose and Compose for Wear OS | Accepted |

To add one, copy an existing file, give it the next number, and keep it under a page. When a decision changes, write a new ADR and mark the old one superseded rather than deleting it.
