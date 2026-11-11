# 0004: Android's recommended architecture, plus a pure Kotlin core

**Status:** Accepted, November 2026. Revised the same month when the stack moved to Kotlin ([0005](0005-kotlin-compose-and-wear-compose.md)).

## Context

We want a structure that's easy to test, easy to follow, and the same in both apps. Android's [guide to app architecture](https://developer.android.com/topic/architecture) recommends a UI layer (screens and ViewModels with unidirectional data flow), a data layer (repositories and data sources), and an optional domain layer for logic that is complex or shared.

## Decision

- **Follow the guide in both apps.** Each screen has a ViewModel that exposes one immutable UI state as a `StateFlow` and takes user actions as function calls. Composables collect the state with `collectAsStateWithLifecycle` and hold no logic.
- **Repositories are the source of truth** for each kind of data, and expose `Flow`s and `suspend` functions. Data sources wrap one outside thing each: the database, the Keystore, the Data Layer, Health Services.
- **Shared rules go in `:core`,** a Kotlin Multiplatform module with no Android imports and no I/O. That covers load per rep, strength estimates, pain guidance and skill paths. It is the domain layer.
- **Dependency injection with Hilt,** Android's recommended library. Hilt 2.59 and later support AGP 9, and code generation runs through KSP. ViewModels are `@HiltViewModel` and are fetched with `hiltViewModel()` inside each Navigation 3 entry, so each one lives exactly as long as its screen.

## Consequences

- **Fast tests.** The training rules are tested without a device. ViewModels are tested with fake repositories and `kotlinx-coroutines-test`.
- **One copy of the rules.** The phone and the watch can't disagree about a number.
- **Some boilerplate.** A ViewModel and a UI state class per screen. We accept that in exchange for screens that are testable and predictable.
