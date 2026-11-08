# ADR-0002: Toolchain and platform

Status: Accepted

## Context

The IDE must run native executables (shell, git, JDK) from the app's data directory, and it is distributed outside Google Play.

## Decision

| Item | Value |
|---|---|
| Android Gradle Plugin | 9.3.1 |
| Gradle | 9.5.0 (the minimum AGP 9.3 requires) |
| JDK | 17 |
| SDK Build Tools | 36.0.0 |
| compileSdk | 37 |
| minSdk / targetSdk | 24 / 28 |
| UI components | Material Components 1.14.0 |
| Distribution | GitHub Releases only |

targetSdk 28: apps targeting API 29 or higher cannot execute files from their writable data directory.

compileSdk is separate from targetSdk. It only decides which APIs the code may call, not how the app behaves at run time.

## Consequences

- The app cannot be published on Google Play, which requires a recent targetSdk. This is accepted.
- Lint checks about an outdated target SDK (`ExpiredTargetSdkVersion`, `OldTargetApi`) are disabled in `:app`.
- Android 14 refuses to install apps that target below API 23, so 28 is safe today. A future platform release raising that floor is a risk to watch.
- Behaviour on the newest Android versions (background limits, process killing) must be tested on real devices. This is an open item.

## Amendment

compileSdk was 36 in the first version of this record. It became 37 because `clotho-android` 0.1.0-alpha01 requires libraries and apps that use it to compile against API 37 or later. Build Tools stay at 36.0.0.
