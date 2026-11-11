# ADR-0023: Pseudo-terminals in native code

Status: Proposed

## Context

A shell needs a terminal: a pseudo-terminal (pty) whose slave side is the shell's standard input, output and error, whose master side the terminal service reads and writes, and a session of its own so that Ctrl-C, hangup and window size changes work. Java and the Android SDK cannot create one: opening `/dev/ptmx` is possible, but `grantpt`, `unlockpt`, `ptsname`, `fork`, `setsid` and the `TIOCSCTTY` and `TIOCSWINSZ` ioctls are not. `ProcessBuilder` gives pipes, not a terminal.

The terminal app of Termux has this code, but its licence is the same unsettled question as for the emulator (ADR-0020), so the code is written here from the POSIX interfaces.

## Decision

- `terminal:pty` is an Android library with a small native part built by CMake (`asm_pty`) and a Java part.
- `pty_core.c` is plain C with no JNI: `asm_pty_spawn` opens `/dev/ptmx`, grants and unlocks it, sets UTF-8 input and the usual line settings, sets the window size, and forks. The child starts a new session, opens the slave as its controlling terminal, makes it standard input, output and error, resets signal handling and the signal mask, closes every other descriptor, changes the directory and runs `execve`. A pipe that closes on exec tells the parent why the exec failed, so a missing program or directory is an error (`ENOENT`) in the caller and not a dead shell. `asm_pty_wait` gives the exit code or minus the signal; `asm_pty_signal` signals the whole process group (the process is its own group leader); reads treat `EIO`, which a master reports when the slave is gone, as the end of the output.
- The child runs only calls that are safe between `fork` and `exec` in a process with many threads (the JVM).
- `pty_jni.c` binds these to `PtyNative`, whose natives throw `IOException` with the errno text. `PtyProcess` is the Java API: `start(path, argv, environment, directory, rows, columns)`, `read` (blocks, -1 at the end), `write`, `resize`, `signal`, `waitFor` (cached, safe from several threads) and `close` (hangup, then kill, then release). The master descriptor is closed only when no thread is inside `read` or `write`, so its number is never reused under a reader.
- The environment is passed whole; nothing is inherited, so the terminal service decides exactly what the shell sees (`PREFIX`, `HOME`, `PATH`, `TERM`, `LD_LIBRARY_PATH` if needed).
- `PtyProcess.isSupported()` is false where the library cannot be loaded, so code and tests degrade instead of crashing.

## Consequences

- The build needs the NDK and CMake. The CI runner has them; AGP downloads what is missing once the licences are accepted, which the CI already does.
- The C code is tested by `pty_core_test.c` in CI (a plain gcc build on the runner: output, exit codes, environment, size and resize, input, working directory, errors, process group signals, Ctrl-C). The Java tests run only where the library can be loaded and are skipped in the Gradle unit tests; they were run against a Linux build of the library.
- Starting programs from the app's data directory needs `targetSdk` 28 or lower (ADR-0002).
- The code is Linux and Android only, by design.

## Open items

- Reading in the service: one thread per session, and how the bytes get to the emulator and to the UI across the process boundary (ADR-0006).
- `posix_spawn` instead of `fork` if it ever gives a measurable win on devices.
- Closing descriptors with `close_range` where the kernel has it.
