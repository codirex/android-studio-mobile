# ADR-0027: The toolchain and JDK management

Status: Proposed

## Context

Everything after v1.0.0 needs a JDK on the device: the Java language server (v1.2.0), Gradle builds (v1.3.0) and the Kotlin tools (v2.0.0). The roadmap says JDK management ships before Gradle builds. The userland is built by [asm-packages](https://github.com/codirex/asm-packages) from termux-packages ([ADR-0007](0007-bootstrap-from-termux-packages.md)), which has `openjdk-17` and `openjdk-21` but no 8 or 11 at the pinned commit. The package repository is signed with the project's key and is already the source `apt` uses inside the userland.

## Decision

- A JDK is an ordinary package of the userland repository. Installing one is `pkg install -y openjdk-17`, run by the package manager the person already has. The IDE does not unpack JDK archives itself.
- `domain:toolchain` holds the model and the use case: `JdkOffer` (a Java version, its package name and where the package puts the JDK below the prefix), `InstalledJdk`, `JdkStatus`, the `JdkLocator` port and `Toolchain`, which lists the offers with their state. Today the only offer is JDK 17 (`lib/jvm/java-17-openjdk`).
- `data:toolchain` implements `JdkLocator` by looking below `files/usr`: a JDK is installed when its `bin/java` exists, and its version is the `JAVA_VERSION` of its `release` file. The prefix name is repeated there because a data module may not depend on `terminal:*` (ADR-0003).
- `feature:toolchain` shows the offers and an Install button. It does not know the terminal: it asks the host through `ToolchainNavigator.installPackage`, and `MainActivity` opens a terminal that types the command (`TerminalFragment.newInstance(directory, command)`), in a session of its own. The person sees the command and its output, and the screen refreshes when they come back.
- Every shell gets `JAVA_HOME` (`TerminalEnvironment`): the newest `lib/jvm/java-NN-*` folder that has a `bin/java`, none while no JDK is installed. The package already links `java` and `javac` into `usr/bin`, so `PATH` needs no change.
- `gradle` is built into the same repository (the official distribution, depending on a JDK 17 or newer), so `pkg install gradle` works. The IDE's own Gradle integration comes with v1.3.0.

## Consequences

- JDK 17 first; other versions follow whatever the package repository gets. JDK 21 can be added to `Toolchain` once it is built. JDK 8 and 11 need packages we do not have (open item).
- Installing is visible and interruptible, at the price of a terminal screen instead of a progress bar. A background installer with progress can replace it later behind the same `ToolchainNavigator` method.
- Nothing is checked after the command ends; the screen simply looks again. A failed install shows as "Not installed".

## Open items

- JDK 8 and 11 (the roadmap names them): either build packages for them or reconsider the list.
- Selecting the JDK a project builds with, once Gradle builds exist.
- Disk space check before installing (a JDK is hundreds of megabytes).
