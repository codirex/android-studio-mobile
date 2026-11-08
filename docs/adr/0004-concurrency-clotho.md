# ADR-0004: Concurrency with Clotho

Status: Accepted. The realm scoping details in point 3 are proposed.

## Context

Heavy work (builds, indexing, terminal I/O) must be owned, cancellable and testable. Clotho is the maintainer's own concurrency library. It provides `Realm`, `Job` and `Lane`, and it is at `0.1.0-alpha01`: its API can still change and it has no `Flow` yet.

## Decision

1. Clotho runs the heavy work.
2. `core:concurrency` is the only module that depends on `clotho-android`. It exposes a `RealmFactory`, installs the main lane and holds the Hilt bindings. Domain modules depend on the Clotho core only and receive realms by injection.
3. Realm scopes:
   - Singleton realm for app-wide work (indexing, build orchestration, JDK and SDK management).
   - One realm per ViewModel, provided by Hilt and closed with `closeAsync()` in `onCleared()`. It survives configuration changes.
   - `AndroidRealms.forLifecycle(viewLifecycleOwner)` only for short work bound to a view. It closes at `ON_DESTROY` and does not survive rotation.
4. The bridge to the UI is thin: work runs on a background lane, then `.then(Lane.main(), ...)` updates a `LiveData`-backed `UiStateHolder`. When Clotho gets `Flow`, the holder's implementation changes and the features do not.

## Consequences

- Changes to Clotho stay inside `core:concurrency`.
- Tests use `clotho-test` (fake clock, manual lanes) through injection.
- Kotlin coroutines are not used in application code. Libraries we depend on may use them internally.
- Because Clotho is alpha, we may need to fix it while building the IDE. That is expected.
