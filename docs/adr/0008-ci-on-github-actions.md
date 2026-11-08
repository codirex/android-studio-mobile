# ADR-0008: Builds and releases run on GitHub Actions

Status: Accepted

## Context

The maintainer cannot build the project locally.

## Decision

- Every push to `main` and every pull request is built by GitHub Actions (`.github/workflows/ci.yml`).
- The debug APK is uploaded as a workflow artifact.
- Until a Gradle wrapper is committed, CI installs Gradle 9.5.0 with `gradle/actions/setup-gradle`.
- Releases are cut from tags and publish a signed APK to GitHub Releases. The release workflow and the signing setup come later.

## Consequences

- Nobody needs a local Android toolchain to contribute a change.
- Feedback on a change takes a CI round trip, so CI must stay fast: Gradle caching is enabled and slow jobs (such as the package builds) live elsewhere.
- CI is the source of truth for whether the toolchain in ADR-0002 works.
