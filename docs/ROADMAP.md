# Roadmap

A release ships only when its scope is done well. The order below was agreed and is revisited if a later step turns out to depend on an earlier one.

## v1.0.0

- Create projects: Android app, Android library, Java library and Gradle plugin, each with several templates.
- Template manager: create and edit templates.
- Open a project, browse its files, open text files.
- Terminal with the essential packages (gh, git, apt, pkg and the other basics) and the Android essentials. It runs in its own process ([ADR-0006](adr/0006-terminal-separate-process.md)) on a userland we build ([ADR-0007](adr/0007-bootstrap-from-termux-packages.md)).
- Editor tabs, auto save and syntax highlighting.
- Later in the 1.x line: suggestions, autocomplete, formatters and parsers.

## v1.2.0

- Java language server and autocomplete through an editor popup, with indexing of class names.
- JDK 1.8, 11 and 17 for use on the device, from GitHub-hosted mobile JDK builds.

## v1.3.0

- Custom environment variables.
- SDK Manager.
- Gradle builds with build output.

## v1.4.0

- API information for classes and their members (since, removed, deprecated and so on).
- Log reader built on Codirex Trace.
- XML language server and autocomplete for res files.

## v1.5.0

- UI designer: layout inflater, resource reference resolution, autocomplete of resource values in the attribute editor, drag and drop, a visual attribute editor and Android widgets.

## v1.6.0

- String translator.
- Asset Studio (drawable and icon maker).

## v2.0.0

- Kotlin language support, Kotlin language server and autocomplete.

## Dependency notes

These are working assumptions to validate before each release starts.

- Everything after v1.0.0 relies on the terminal and the userland from v1.0.0.
- v1.2.0 needs a JDK on the device; JDK management therefore ships in v1.2.0, before Gradle builds in v1.3.0.
- v1.5.0 (UI designer) needs resources and `android.jar`, which come from the SDK Manager in v1.3.0, and the resource model from the XML work in v1.4.0.
- v2.0.0 reuses the language server and indexing framework built for Java in v1.2.0.
