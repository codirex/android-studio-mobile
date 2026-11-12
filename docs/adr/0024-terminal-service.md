# ADR-0024: The terminal service and its protocol

Status: Proposed

## Context

[ADR-0006](0006-terminal-separate-process.md) puts the terminal in its own process, so that shells and tools outlive the UI, and leaves open how output gets to the screen. The terminal emulator ([ADR-0020](0020-terminal-emulator.md)) is plain Java and keeps the screen state; the pseudo-terminal ([ADR-0023](0023-pty-native.md)) gives raw bytes.

## Decision

- The service process owns the programs, not the screen. It reads the raw bytes of each session and keeps the most recent 1 MiB of them in a ring buffer (`OutputBuffer`). Every byte has an absolute offset (the number of bytes the session printed before it).
- The UI process owns one emulator per session. It attaches with the offset it has already seen (0 for a new screen): the service sends what it still keeps from there, then the live output, under one lock, so the UI sees an ordered stream with no gap and no repeat. If the first chunk starts after the offset the UI asked for, older output was dropped and the UI starts its emulator from a clear screen. That is also how a UI process that was killed gets its terminals back. The screen is rebuilt by replay, so no emulator state crosses the process boundary and the emulator stays out of the service.
- `terminal:protocol` holds three AIDL interfaces and nothing else. `ITerminalService` has `isUserlandInstalled`, `installUserland`, `cancelInstall`, `openSession`, `sessionIds`, `attach`, `detach`, `write`, `resize`, `signal` and `closeSession`. `ITerminalListener` is `oneway` (`onOutput`, `onExit`), so a slow or dead UI never blocks reading from the terminal; a listener that throws is dropped. `IInstallListener` is `oneway` too. Chunks are at most 16 KiB, well under the Binder transaction limit. A dedicated shared-memory or socket channel can replace this later without changing the model, if measurements ask for it.
- `terminal:service` has the logic: `Session` (reader thread, waiter thread, listeners, replay), `SessionManager` (ids, which are never reused) and `OutputBuffer`. It sees programs through a small `TerminalProcess` interface, implemented by `PtyProcess` in production and by a fake in the unit tests, so all of it is tested on the JVM.
- When the shell ends, the exit is reported after a short wait for the last output. If something the shell started still holds the terminal, the session closes it (hangup, then kill) as closing a terminal window does, and then reports the exit. The session stays listed, so a UI that attaches later sees the end of the output and the exit code, until `closeSession`.
- Threads are plain daemon threads: the service process does not use Clotho or Hilt for this, which keeps the second process small and fast to start.
- The Android side is `TerminalService`, a foreground service declared in the manifest of `terminal:service` with `android:process=":terminal"`, and `TerminalBinder`, which checks what the UI sends and hands it to the sessions. The UI starts the service with `TerminalService.start` and binds with `TerminalService.intent`. The service stops itself when its last session is closed; sessions that ended stay listed until the UI closes them.
- The shell and its environment come from the files directory (`TerminalEnvironment`): `usr/bin/bash` as a login shell, `PREFIX`, `HOME`, `PATH`, `TMPDIR`, `TERM`, `LANG` and the termux-exec preload library when the userland has it. Nothing is inherited from the app process.
- The service also installs the userland (`UserlandSetup`): the archive for the ABI of the device is downloaded and verified (ADR-0022), installed, and deleted. One installation runs at a time and any number of callers follow it through `IInstallListener`; it can be cancelled before the extraction starts.

## Consequences

- Sessions survive the UI. Reattaching costs one replay of at most 1 MiB.
- Terminals that print very fast fill the buffer and drop their oldest output; the UI is told by the offset gap.
- `feature:terminal` will need the view and the emulator in the UI process. Rule 2 of ADR-0003 lets a feature depend on `terminal:protocol` only, so a later ADR will add a `terminal:view` module (the view and a client of the protocol) as a second allowed dependency of features.
- AIDL needs `buildFeatures.aidl` in `terminal:protocol`.
- The Application class of the app also runs in the `:terminal` process. Its start-up work (Hilt, initializers) runs there too; if that costs too much, the Application will skip what the service does not need.

## Open items

- Several UIs attached to one session at once are supported by the service; whether the app needs it is not decided.
- Notification content and the "exit" action of the foreground service.
- Output of a session that nobody has attached for a long time: it is only the 1 MiB buffer, by design.
- Whether the termux-exec preload library of our packages needs more variables than the ones set here; this is found by running the first shell on a device.
