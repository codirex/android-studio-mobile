# ADR-0011: Domain ports and low-level file access

Status: Proposed

## Context

[ADR-0003](0003-module-layering.md) lets `core:*` depend only on `core:*`, so a core module cannot implement an interface that lives in `domain:*`. The project features need file access, and the domain must stay free of Android and of I/O.

## Decision

- `core:fs` offers **mechanisms**: `FileOperations` with atomic writes, a path resolver that cannot escape its root, symlink-safe deletion and sorted listing. It knows nothing about projects.
- `domain:*` modules define **ports** in their own vocabulary (for example `ProjectRepository`). A port never mentions `java.io.File`.
- `data:*` modules implement the ports on top of `core:fs`, and run the blocking work in Clotho jobs on an I/O lane.
- `domain:common` holds only what several domain modules really share. Today that is `Validation`.
- Domain value objects validate themselves and expose `validate(String)` so the UI can check input while the user types. Error codes are stable keys that the UI maps to localized text.
- All file code uses `java.io`. `java.nio.file` needs API 26 and the app supports API 24.

## Consequences

- One more hop (feature, domain port, data implementation, core:fs), in return for domain logic that is tested on the JVM with no file system.
- Every method of `FileOperations` blocks. Calling it from the main thread is a bug; a StrictMode check in debug builds is a candidate follow-up.
- Symlink detection relies on canonical paths, because `java.nio.file.Files.isSymbolicLink` is not available on the lowest API levels.

## Open items

- Scoped storage on newer Android versions: which directories projects may live in, and how files outside the app directory are reached. This decides the first `data:project` implementation.
