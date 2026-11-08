# ADR-0003: Module layering

Status: Proposed

## Context

The product will grow from a file editor to an IDE with language servers, a build system front end and a visual designer. Without firm boundaries the code will tangle, as it does in many large Android projects.

## Decision

The project is split into the layers described in [ARCHITECTURE.md](../ARCHITECTURE.md): `app`, `feature:*`, `domain:*`, `data:*`, `core:*` and the `terminal:*` subsystem.

Rules:

1. Dependencies point inward only: `app` to `feature` to `domain`; `data` implements `domain`.
2. A feature module never depends on another feature module.
3. `domain:*` modules contain no Android imports. They are plain JVM modules and may mix Java and Kotlin.
4. Only `core:concurrency` depends on `clotho-android` ([ADR-0004](0004-concurrency-clotho.md)).
5. Modules expose a small public API; implementation classes are package-private (Java) or `internal` (Kotlin).

The rules are enforced by convention plugins and a CI check, not by review alone.

## Consequences

- More modules, so more Gradle configuration. Convention plugins in `build-logic/` keep each module build file short.
- Features communicate through domain interfaces and navigation contracts owned by `app`.
- Domain code is testable on the JVM without an emulator.

## Open items

- Navigation approach (single activity with Fragments and the Navigation component, or something lighter).
- Whether View Binding is mandatory (leaning yes).
- The exact tool that enforces the dependency rules.
