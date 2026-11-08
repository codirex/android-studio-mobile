# ADR-0012: Where projects live and how they are identified

Status: Proposed

## Context

The terminal, Gradle and git need a real POSIX file system: executable bits, symbolic links, atomic renames. Shared storage on Android is a FAT-like layer that offers none of that, and it needs broad file access permissions on recent versions.

## Decision

- Projects live in the app's private directory, `files/projects/<name>`, which is inside the same prefix the terminal uses ([ADR-0006](0006-terminal-separate-process.md), [ADR-0007](0007-bootstrap-from-termux-packages.md)).
- Export and import as a zip file come later, as the way to move a project in or out of the app. A shared-storage location may be added as an extra choice. Neither changes `domain:project`, whose port never mentions `File`.
- A project's identity is its name, and its name is its folder name. Names are validated as safe folder names ([ProjectName](../../domain/project/src/main/java/org/codirex/asm/domain/project/ProjectName.java)).
- A folder is a project only if it contains `.asm/project.properties` with `format`, `package`, `type` and `created`. The format is a Java properties file, so it needs no library and reads the same on the JVM and on Android. `format` is `1`; a different value is rejected, never guessed.
- Listing skips broken or foreign folders. Deleting refuses any folder without the metadata file, so a bug cannot delete an arbitrary directory.
- `create` makes an empty project. Source files arrive when templates are applied on top, in a later feature.

## Consequences

- Projects disappear when the app's data is cleared. The UI must say so, and export must be easy to find.
- Renaming a project is a folder rename plus nothing else, because nothing else stores the name.
- Two names that differ only in case are different projects on the app's file system; exported zips may collide on a case-insensitive system.

## Open items

- Last-opened time and favourites: stored per project, in the metadata file or in preferences.
- Zip format and how template files are layered on creation.
