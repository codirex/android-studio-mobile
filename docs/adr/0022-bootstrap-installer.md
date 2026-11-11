# ADR-0022: Installing the userland from a bootstrap archive

Status: Proposed

## Context

ADR-0007 builds a bootstrap archive per architecture (`bootstrap-<arch>.zip`, the contents of the prefix `/data/data/org.codirex.asm/files/usr`) and attaches it to a release of `codirex/asm-packages`. The app must get it onto the device before the terminal can start a shell, and a half installed prefix is worse than none: the shell would start and fail in strange ways.

## Decision

- `terminal:bootstrap` is an Android library (it needs `core:fs`, and `android.system.Os` for links) with no Hilt and no UI. Its `BootstrapInstaller` installs from an `InputStream` of the archive, so where the bytes come from (a download, a file, a test) is not its concern.
- The archive does not store symbolic links; `SYMLINKS.txt` lists them as `target←path`, as in Termux's `generate-bootstraps.sh`. The installer extracts into `files/usr-staging`, creates the links from the list, makes `files/home` and `usr/tmp`, and moves the staging folder to `files/usr` in one step. The prefix exists only complete, so `isInstalled` is "the prefix is a folder". A crash leaves at most a staging folder, which the next install deletes. A failed install deletes its staging folder.
- Every entry name and link path is resolved with `FileOperations.resolve` and may not leave the staging folder; absolute names, backslashes and NUL characters are refused. Limits: 100,000 entries, 2 GiB extracted, 8 MiB for the link list. An archive without `SYMLINKS.txt` or without programs in `bin/` is not a bootstrap archive.
- A zip archive carries no permissions, so files under `bin/`, `libexec`, `lib/apt/apt-helper`, `lib/apt/methods` and `etc/termux/bootstrap/` get the owner's executable bit (the rule of the Termux app).
- Symbolic links are created through the `SymlinkCreator` interface: `OsSymlinkCreator` on devices, `java.nio.file.Files` in the tests (nio links need API 26, the app supports 24).
- `BootstrapArch` maps `Build.SUPPORTED_ABIS` to the archive to download (`arm64-v8a` to `aarch64`, `armeabi-v7a` to `arm`, `x86_64`, `x86` to `i686`).
- The archive's own second stage script (`etc/termux/bootstrap/termux-bootstrap-second-stage.sh`) finishes the installation on the first shell start through the fallback in `etc/profile.d`; the installer does not run it.

## Consequences

- Reinstalling or updating means deleting `usr` first; that is a decision of the update strategy, not of this module.
- The installer is tested on the JVM with archives built in memory: extraction, links, permissions, progress, unsafe names, bad link lines, truncated archives, a failing link creator, a stale staging folder.

## Open items

- Download of the archive from the release, with a checksum, progress and resume; the checksum file is published next to the archive.
- Where the install runs (the terminal service, before the first session) and how its progress reaches the UI.
- Update strategy for an installed userland.
