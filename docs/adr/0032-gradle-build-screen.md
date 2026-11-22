# ADR-0032: Gradle build screen with parsed output

Status: Accepted

## Context
ADR-0029 ran Gradle tasks in a terminal. That shows everything and explains nothing: a compile error is one line among hundreds, and the person has to find the file by hand.

## Decision
- **Output over a socket.** The terminal service gets `runGradle(projectDirectory, task)`. It starts `./gradlew` (or the userland's `gradle`) with `--console=plain` in the project folder, using the same environment as the terminal (JAVA_HOME, ANDROID_HOME, aapt2 override), and returns a socket carrying stdout and stderr merged. The socket ends with the build; closing it stops the build. The UI names a task and a folder, never a program; the service accepts only task names `GradleTask` accepts and only folders directly under `files/projects`.
- **Parsing in the domain.** `GradleOutputParser` (`domain:toolchain`, plain JVM) reads lines and keeps a `BuildReport`: tasks, javac and kotlinc errors and warnings with file, line and column, the "What went wrong" text, and the result with its duration. Unknown lines are ignored.
- **Screen.** `feature:build` (`BuildFragment`, `BuildViewModel`) connects to the service, reads the socket off the main thread, feeds the parser and publishes a state at most every 150 ms. It shows the status, the counts, the failure text and the problems. Touching a problem with a place asks the activity (`BuildNavigator`) to open the file in the project's editor. When a build ends without an error the parser understands, the last lines of raw output are shown so nothing is hidden.
- **Lifetime.** The build lives as long as the screen; leaving it closes the socket and stops the build.
- **Layering.** A feature may use `:terminal:view` and `:terminal:protocol` (ADR-0025), so `feature:build` needs no new rule.

## Consequences
- Builds no longer run in a terminal by default; the terminal stays available from the menu.
- Opening a problem opens the file, not yet the line.
- Output formats the parser does not know (other plugins' messages) appear only in the raw tail.
