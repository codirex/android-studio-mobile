# ADR-0014: The editor, its tabs and the project workspace

Status: Proposed

## Context

v1.0.0 needs to open text files, edit them in tabs and save them automatically (ADR-0005 chose the Sora editor). Opening a project must show the file tree and the editor together, because a developer jumps between them all the time, and tabs must survive navigation inside a project.

## Decision

- `domain:editor` (pure JVM) holds `EditorTabs`, an immutable value: the open files by relative path and the active one. Opening a file that is open selects it. Closing the active tab selects its right neighbour, or the left one if there is none. It also holds the port `ProjectDocuments` (read and write a text file of a project) and `DocumentException` with the reasons not found, not allowed, not text, too large and storage.
- `data:project` implements the port on top of `core:fs`. A file larger than 2 MB, or whose first characters contain a NUL, is refused. Writing is atomic (ADR-0011). The internal `.asm` folder cannot be read or written, also not through `..` segments. Resolving a project path is shared with the file tree in `ProjectLocator`.
- `feature:editor` shows a tab bar and one Sora `CodeEditor`. The view model keeps the text of every open document in memory and tells the screen which document the editor must show through a revision counter, so the editor widget is only reset when the shown document changes.
- The editor widget holds the text of the active tab only. The screen hands a snapshot of that text to the view model before every change of tab and when it pauses. Typing only marks the tab as changed; the text is read when a save runs, so a key press costs nothing.
- Auto-save: one second after the last change, on tab change, on closing a tab and when the screen pauses. Saves of one file never overlap: a save requested while another runs is queued and runs afterwards. A tab shows a dot while its file has unsaved changes.
- The project workspace is composed by the app, not by a feature: `WorkspaceFragment` in `:app` holds `FileTreeFragment` in a navigation drawer and `EditorFragment` as content. Features keep knowing nothing about each other (ADR-0010): the tree reports a tapped file through `FileTreeNavigator`, the editor asks for the tree through `EditorNavigator`, and `MainActivity` connects both to the workspace.
- The editor follows the system day or night mode with the Sora schemes GitHub and Darcula.

## Consequences

- Undo history is lost when the user switches tabs, because one editor widget is reused. Keeping a `Content` per tab can be tried later.
- Closing the screen within a second of typing relies on the pause save. That save runs in the view model's realm; if the screen is destroyed in the same moment it might be cancelled, and the old file stays intact. A saver owned by the application would remove this risk.
- Syntax highlighting, formatting and completion are not part of this decision.

## Open items

- Reload when a file changes on disk while it is open.
- Restoring the open tabs after the app restarts.
- New file, rename and delete from the tree.
