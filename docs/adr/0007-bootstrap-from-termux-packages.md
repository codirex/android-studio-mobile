# ADR-0007: Userland built from termux-packages

Status: Accepted

## Context

The terminal needs a shell, git, gh, apt and pkg, and the Android build tools. Prebuilt Termux packages are compiled for the prefix `/data/data/com.termux/files/usr` and do not work under another package name.

## Decision

We build the packages ourselves on GitHub Actions from the open termux-packages sources, with the prefix `/data/data/org.codirex.asm/files/usr`.

The CI produces, per architecture, a bootstrap archive that the app installs on first run and an apt repository that `apt` and `pkg` use afterwards.

## Consequences

- We own the build, so we ship exactly the packages we need and update them on our schedule.
- Builds are slow and large. Only the packages we need are built; the set is reviewed per release.
- Many packages are GPL or similar. License compliance for redistribution must be reviewed before the first release.

## Open items

- Where it lives. A separate repository (a fork of termux-packages, for example `codirex/asm-packages`) is recommended so that this repository's CI stays fast.
- Architectures: aarch64 and armv7 for devices, x86_64 for emulators.
- The minimal package set for v1.0.0.
- Hosting of the apt repository (GitHub Pages or release assets) and the signing key.
- Confirm how termux-packages configures a custom app package name and prefix (believed to be `scripts/properties.sh`).
- Update strategy for installed bootstraps.
