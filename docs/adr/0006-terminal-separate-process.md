# ADR-0006: The terminal runs in a separate process

Status: Accepted

## Context

The terminal hosts shells and long-running tools (package installs, later Gradle builds and language servers). They use a lot of memory and must survive the UI being closed or killed.

## Decision

The terminal runs in a dedicated foreground service declared with `android:process=":terminal"`.

- `terminal:protocol` holds the IPC contract (AIDL/Binder). It is the only module that `feature:terminal` and `terminal:service` share.
- `terminal:service` owns pseudo-terminals, child processes and sessions.
- `terminal:emulator` implements terminal emulation and rendering.
- `terminal:bootstrap` installs and updates the userland ([ADR-0007](0007-bootstrap-from-termux-packages.md)).

## Consequences

- Sessions keep running when the UI process dies and can be reattached.
- The service must run as a foreground service. API 28 requires the `FOREGROUND_SERVICE` permission, and the service must call `startForeground` shortly after it starts.
- Process isolation costs an IPC layer: every interaction crosses a Binder boundary, and output streaming needs a design that avoids copying large buffers.
- Hilt needs a separate component graph for the second process.

## Open items

- Output streaming design (ring buffer, shared memory or a socket).
- Terminal emulator: reuse Termux's terminal-emulator library or write our own. Its license must be verified before we depend on it.
- Whether builds and language servers use the same service or get their own processes.
