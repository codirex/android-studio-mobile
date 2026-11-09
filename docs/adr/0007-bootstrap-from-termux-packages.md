# ADR-0007: Userland built from termux-packages

Status: Accepted

## Context

The terminal needs a shell, git, gh, apt and pkg, and the Android build tools. Prebuilt Termux packages are compiled for the prefix `/data/data/com.termux/files/usr` and do not work under another package name.

## Decision

We build the packages ourselves on GitHub Actions from the open termux-packages sources, with the prefix `/data/data/org.codirex.asm/files/usr`.

The CI produces, per architecture, a bootstrap archive that the app installs on first run and an apt repository that `apt` and `pkg` use afterwards.

The work lives in the separate public repository `codirex/asm-packages` (Apache-2.0), so this repository's CI stays fast. It is not a fork: it pins an upstream termux-packages commit (`termux-packages.ref`) and applies a small overlay script that sets `TERMUX_APP__PACKAGE_NAME` in `scripts/properties.sh`. Updating upstream means changing the pin.

## Findings

- `scripts/properties.sh` is the single place that defines the app package name and the prefix. `org.codirex.asm` is within the upstream length limits.
- The upstream Docker builder image bakes `properties.sh` in at image build time, so the CI builds the builder image itself from the patched checkout instead of pulling `ghcr.io/termux/package-builder`.
- A probe build of one package for aarch64 succeeded and its `.deb` uses the `org.codirex.asm` prefix.
- `generate-bootstraps.sh` downloads prebuilt packages from an apt repository given by `--repository`, so the bootstrap is generated from our own repository after the packages are built. Dependencies must be built from source because prebuilt ones target `com.termux`.
- `termux-keyring` ships the Termux signing keys. Our repository needs its own key, and the keyring package must carry our public key.

## Consequences

- We own the build, so we ship exactly the packages we need and update them on our schedule.
- Builds are slow and large. Only the packages we need are built; the set is reviewed per release.
- Many packages are GPL or similar. License compliance for redistribution must be reviewed before the first release.

## Open items

- Architectures: aarch64 and armv7 for devices, x86_64 for emulators (i686 is optional).
- The minimal package set for v1.0.0 (the base set is in `packages/bootstrap.txt`; git and gh are added next).
- Hosting of the apt repository (GitHub Pages or release assets) and the signing key (private key only as a GitHub secret).
- Make `termux-keyring` and the default apt sources point to our key and repository.
- Update strategy for installed bootstraps.
