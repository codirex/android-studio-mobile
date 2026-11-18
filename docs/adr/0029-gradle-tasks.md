# ADR-0029: Running Gradle tasks from the IDE

Status: Proposed

## Context

After the JDK ([ADR-0027](0027-toolchain-jdk.md)) and the environment variables ([ADR-0028](0028-environment-variables.md)) the userland can run `gradle`: the package is in our repository and the terminal sets `JAVA_HOME`. The roadmap's Gradle integration (v1.3.0) will eventually show build output in the IDE, parse errors and offer a task tree. Before that, a person needs a way to start a build of the open project without typing the command.

## Decision

- `domain:toolchain` gets `GradleTask`: a task name or path (`build`, `:app:assembleDebug`) checked against letters, digits and `_ : . -` (not starting with a dash), and the command line that runs it. `GradleTask.common()` lists `build`, `assembleDebug`, `test`, `clean` and `tasks`.
- The command is `sh ./gradlew <task>` if the project has a `gradlew` (run through `sh`, so it needs no execute bit), otherwise `gradle <task>` of the userland. The name check keeps anything that is shell syntax out of the command.
- The app offers "Run Gradle task" in the menu of an open project. The chosen task runs in a new terminal session in the project folder (`TerminalFragment.newInstance(directory, command)`, as the package installer does), so the person sees the real output and can interrupt it.

## Consequences

- No parsing, no progress, no cancellation of its own: it is a terminal. A build screen with output parsing replaces this later behind the same menu item.
- Android projects need the Android SDK (`ANDROID_HOME`) and `aapt2`, which the userland does not have yet (no `aapt2` package at the pinned termux-packages commit). `build` of a Java library or a Gradle plugin works; `assembleDebug` of an app will fail until the SDK manager exists (open item).
- Only the five common tasks are offered; a custom task name is possible in the terminal.

## Open items

- SDK manager and `aapt2` (Android builds).
- Choosing the JDK per project, once the repository has more than one JDK.
- A build screen with parsed output and a task tree.
