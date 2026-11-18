# ADR-0028: Custom environment variables of the terminal

Status: Proposed

## Context

Builds and tools read their settings from environment variables: `JAVA_HOME`, `GRADLE_OPTS`, `ANDROID_HOME`, proxy settings. The roadmap lists "custom environment variables" for v1.3.0, next to the SDK manager and Gradle builds. The terminal runs in its own process (ADR-0024) and builds the whole environment of a shell itself (`TerminalEnvironment`); nothing is inherited.

## Decision

- The person keeps a list of name/value pairs. Every shell started afterwards gets them, added after the defaults, so a custom variable can change a default (`LANG`, `PATH`, `JAVA_HOME` of the detected JDK).
- `domain:environment` holds the rules: `EnvironmentVariable` (a name of letters, digits and underscores that does not start with a digit; a value of at most 4096 characters without NUL), `EnvironmentStore` (the port), `EnvironmentVariables` (list sorted by name, set, remove) and `EnvironmentException` with a reason the screen can show.
- `HOME`, `PREFIX`, `SHELL` and `LD_PRELOAD` are reserved: the terminal cannot work without them, and a wrong `LD_PRELOAD` makes every program fail. Everything else may be changed, `PATH` included.
- `data:environment` implements the store as `files/environment.properties`, a UTF-8 file in the format of `java.util.Properties`, written through a temporary file and a rename. The format is a contract: `terminal:service` reads the same file when a session starts. A data module may not be used by `terminal:*` and the other way round (ADR-0003), so the file name and the format are repeated on both sides, and both sides ignore an entry that breaks the rules (a hand-edited file cannot break the terminal).
- `feature:environment` shows the list with an Add button; a tap changes or removes a variable. The app opens it from the menu of the project list.

## Consequences

- A change applies to sessions started after it; open sessions keep their environment (a process cannot change it from outside).
- Values are plain text. Secrets (tokens) should not be put here: the file is in the private files directory, but it is not encrypted.
- The reserved names exist twice (domain and terminal service). A test on each side keeps them honest; moving them into a shared contract module is possible if a third consumer appears.

## Open items

- Environment per project (a variable that only applies in one project's terminal and builds).
- Import from a `.env` file.
- Applying a change to open sessions by typing `export` into them (rejected for now: it would be typed into whatever program is running).
