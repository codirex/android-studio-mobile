# ADR-0037: Gradle sync for the Java language server

Status: Accepted

## Context

jdt.ls (ADR-0031) knew only the JDK: with Gradle import switched off it proposed `jdk.*` and `sun.*`
and nothing from `android.*`, the imports of the file or the libraries of the build. Its own Gradle
import (Buildship) does not understand Android projects and would need a second Gradle, memory and
network.

## Decision

1. **One init script.** `asm-sync.gradle` (an asset of `feature:editor`) is copied to
   `~/.gradle/init.d/` of the userland before every sync. It adds the task `asmSync` to every project.
   For each module that has sources or the Android plugin the task writes `.project` and
   `.classpath` (Eclipse project files, which jdt.ls imports on its own):
   - source folders: `src/main/java`, `src/debug/java` and the generated folders that exist
     (BuildConfig, annotation processing output, data binding);
   - libraries: the boot classpath of the SDK (`androidComponents.sdkComponents.bootClasspath`,
     else `$ANDROID_HOME/platforms/android-N/android.jar`), the jars of the module's
     `debugCompileClasspath` (AARs as their `classes.jar`, through the artifact type
     `android-classes-jar`; `compileClasspath` and `jar` for plain Java modules) and the `R.jar` of
     the debug build when there is one;
   - project dependencies as project references, not as jars, so nothing has to be built first;
   - an Android module has no JRE container: `java.*` comes from `android.jar`, so the JDK's
     `jdk.*` and `sun.*` are not proposed.
   The task prints `ASM_SYNC_OK` per module and is not compatible with the configuration cache.
2. **Run through the service.** `GradleSync` (in `feature:editor`) runs `asmSync` with the existing
   `runGradle` of the terminal service (ADR-0032), reads the output, and counts the sync as done when
   there is an `ASM_SYNC_OK` line and `BUILD SUCCESSFUL`. It writes `.asm/sync.stamp`.
3. **When.** The editor syncs when a project opens and the stamp is missing or older than the Gradle
   files (`build.gradle*`, `settings.gradle*`, `gradle.properties`, `libs.versions.toml`, up to three
   levels deep). A failed sync is stamped too, so it is not repeated on every open; the palette command
   "Sync Gradle project" runs it by hand. If Gradle is not installed nothing runs and no stamp is
   written. The status line shows "Gradle sync" while it runs; on failure a snackbar offers the build
   screen with the task.
4. **Restart.** After a successful sync the editor stops the Java server and starts a new one, which
   imports the project files at startup.

## Consequences

- A sync needs Gradle, the JDK 17 of the userland, the SDK platform and network for the libraries.
- `R` and generated classes appear only after a first build of the module (their folders and `R.jar`
  must exist); a sync after the first build picks them up.
- `.project`, `.classpath` and `.asm/` are written into the project folder; add them to `.gitignore`.
- The init script is also loaded by ordinary builds, where it only registers the task.
- Kotlin sources, test source sets, flavors and variants other than debug are not in the classpath.
- Unverified on a device: the artifact view for AARs with AGP 9, and jdt.ls importing the files.
