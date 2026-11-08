# Android Studio Mobile (ASM)

A complete Android development environment that runs on Android itself: project creation, a code editor, a real terminal, builds and, later, language servers and a visual UI designer.

> **Status: pre-alpha, architecture phase.** There is no release yet. The first milestone is [v1.0.0](docs/ROADMAP.md#v100).

## Goals

- Build, edit and run Android and Java projects on a phone or tablet.
- A clear modular architecture that stays maintainable for years (see [ARCHITECTURE](docs/ARCHITECTURE.md) and the [decision records](docs/adr/README.md)).
- Tooling that is installed once and then runs locally on the device.

## Platform

| | |
|---|---|
| Package | `org.codirex.asm` |
| minSdk / targetSdk | 24 / 28 |
| Distribution | GitHub Releases (not Google Play) |

targetSdk 28 is deliberate: apps targeting API 29 or higher cannot execute files from their writable data directory, which the terminal and the toolchain rely on. See [ADR-0002](docs/adr/0002-toolchain-and-platform.md).

## Stack

| Area | Choice |
|---|---|
| Build | AGP 9.3.1, Gradle 9.5.0, JDK 17, Build Tools 36 |
| UI | XML Views, Material Components 1.14.0 |
| Editor | Sora Editor |
| DI | Hilt (Dagger) |
| Architecture components | AndroidX ViewModel, Lifecycle, Preferences, Startup |
| Concurrency | [Clotho](https://github.com/codirex/clotho) |
| Log reading (v1.4.0) | [Codirex Trace](https://github.com/codirex/codirex-trace) |

Library versions are pinned in `gradle/libs.versions.toml` when each library is first used.

## Building

Builds run on GitHub Actions only. Download the `asm-debug-apk` artifact from the latest successful run in the Actions tab.

## Legal

Copyright 2026 Ahmed Masoud. Licensed under the [Apache License 2.0](LICENSE).

Android and Android Studio are trademarks of Google LLC. This project is independent and is not affiliated with or endorsed by Google.
