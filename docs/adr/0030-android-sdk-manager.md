# ADR-0030: The Android SDK manager

Status: Proposed

## Context
Building an Android project needs two pieces of the Android SDK: a platform (`android.jar` of an API level) and the build tools. Google publishes both as zip archives, listed in a catalog (`repository2-3.xml`) with a SHA-1 and a license for each. The IDE has no way to run Google's `sdkmanager` on the device (it is a tool for desktops), so it has to do the same job itself.

## Decision
- `domain:sdk` holds the model (package, archive, license, entry), the catalog parser and the use cases (`SdkManager`: list, accept a license, install, remove). `SdkSource` (catalog and downloads) and `SdkStorage` (installed packages, accepted licenses) are ports.
- `data:sdk` implements them: `HttpSdkSource` over HTTPS from `https://dl.google.com/android/repository/`, `FileSdkStorage` in `files/sdk`. `feature:sdk` is the screen.
- Only `platforms;android-N[-extM]` and released `build-tools;N.N.N` are offered. Previews are not.
- The person accepts the license in the app before the first download. What was accepted is stored as the SHA-1 of its text, so a changed text asks again.
- An archive is checked against the SHA-1 of the catalog before it is unpacked; a mismatch discards it. Archive addresses must be relative to the repository; entries that would leave the package folder are refused; the size and number of entries are capped.
- A package is unpacked beside its final folder and moved in place; the marker `.asm-package` is written last, so an interrupted install leaves nothing that looks installed. An older version is kept until the new one is in place.
- The terminal sets `ANDROID_HOME` and `ANDROID_SDK_ROOT` to `files/sdk`. When the userland has `aapt2` (package `aapt`), the terminal also sets `ORG_GRADLE_PROJECT_android.aapt2FromMavenOverride` to it, because the `aapt2` that comes with the Android Gradle plugin is built for x86-64.

## Consequences
- The build-tools archive for Linux contains x86-64 programs that cannot run on the device. They are installed anyway: the jars (`d8`, `apksigner`) and the layout are what builds need; the programs are replaced by the userland's.
- The catalog parser is written from the documented format and tested on a sample; it has not yet been run against the real catalog.
- The licenses are Google's text, shown unchanged.

## Open items
- Check the whole flow on a device: catalog, download, install, an Android build with the override.
- Platform tools and the NDK are not offered.
- Resume of an interrupted download.
