# ADR-0003: Module layering

Status: Accepted

## Context

The product will grow from a file editor to an IDE with language servers, a build system front end and a visual designer. Without firm boundaries the code will tangle, as it does in many large Android projects.

## Decision

The project is split into the layers described in [ARCHITECTURE.md](../ARCHITECTURE.md): `app`, `feature:*`, `domain:*`, `data:*`, `core:*` and the `terminal:*` subsystem.

Rules:

1. Dependencies point inward only: `app` to `feature` to `domain`; `data` implements `domain`.
2. A feature module never depends on another feature module.
3. `domain:*` modules contain no Android imports. They are plain JVM modules and may mix Java and Kotlin.
4. Only `core:concurrency` depends on Clotho ([ADR-0004](0004-concurrency-clotho.md)). It exposes Clotho types in its API, so features see them transitively but never name the dependency.
5. Modules expose a small public API; implementation classes are package-private (Java) or `internal` (Kotlin).

Allowed dependencies by layer:

| Layer | May depend on |
|---|---|
| `app` | everything |
| `feature:*` | `domain:*`, `core:*`, `terminal:protocol` |
| `domain:*` | `domain:*` |
| `data:*` | `domain:*`, `core:*` |
| `core:*` | `core:*` |
| `terminal:*` | `terminal:*`, `core:*` |

Enforcement:

- Convention plugins in `build-logic/` (`asm.android.application`, `asm.android.library`, `asm.jvm.library`) give every module the same platform settings. A `domain:*` module uses only `asm.jvm.library`.
- `tools/check_layering.py` reads the build files and sources and fails the first CI job on any violation: forbidden edges, cycles, Android imports in `domain:*`, Clotho outside `core:concurrency`, a second application module.

## Consequences

- More modules, so more Gradle configuration. Convention plugins keep each module build file short.
- Features communicate through domain interfaces and navigation contracts owned by `app`. `data:*` is wired to `domain:*` interfaces in `app` through Hilt.
- Domain code is testable on the JVM without an emulator.
- The check is text based, so a dependency declared by a mechanism it does not parse (for example type-safe project accessors) would escape it. The project uses `project(":x")` notation only.

## Open items

- Navigation approach (single activity with Fragments and the Navigation component, or something lighter).
- Whether View Binding is mandatory (leaning yes).
- Whether `domain:*` may use Clotho's pure-JVM core artifact for async contracts. Today the rules forbid it.
