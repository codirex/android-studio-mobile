# ADR-0035: Command palette

Status: Accepted

## Context

The design has a command palette: one field that finds files and runs commands.

## Decision

1. `domain:palette` (pure Java) holds `FuzzyMatcher` (letters in order, word starts, file name
   first, shorter first) and `FileIndex` (the files of a project, without tool folders and Gradle
   build output). Both are tested on the JVM.
2. The palette screen is in `app`, a dialog fragment. It is composed there because it uses commands
   of several features (ADR-0010): `MainActivity` implements `CommandPaletteFragment.Host` and says
   what the commands are, so no feature depends on another.
3. An empty field lists the commands; text searches files and commands together; a leading `>`
   searches commands only. Symbol search (`@`) comes with the language server's workspace symbols.
4. The palette opens from a search button in the top bar while a project is open.

## Consequences

Key shortcuts are not shown: the IDE has no hardware keyboard handling yet.
