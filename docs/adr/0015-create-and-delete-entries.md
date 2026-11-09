# ADR-0015: Creating and deleting files and folders

Status: Proposed

## Context

The file tree only showed files. A project needs new files and folders and a way to remove them. Renaming and moving are left for later because they need a rename operation in `core:fs` and a decision on how open tabs follow a renamed file.

## Decision

- The port `ProjectFiles` (domain:filetree) gets `create(project, parentPath, name, directory)` and `delete(project, path)`. Failures keep using `ProjectException`: `ALREADY_EXISTS` when the name is taken, `STORAGE` for everything else, including an invalid name or a path outside the project.
- `EntryNames` in the domain decides which names are valid: not empty, at most 255 characters, not `.` or `..`, and without `/` or NUL. The dialog checks it before asking, and the data layer checks it again, because a name is a path segment and must never move the file somewhere else.
- `data:project` resolves every path through `ProjectLocator`, so `..` and links cannot leave the project. The internal `.asm` folder cannot be created into, deleted or replaced. The project root cannot be deleted.
- The tree offers the actions on long press (folders: new file, new folder, delete; files: delete) and two buttons at the top for the project root, so an empty project can get its first file.
- Deleting asks for confirmation. Creating a file opens it in the editor.
- The tree view model reports what happened as a one-time event in its state (`TreeEvent`): a file was created and should be opened, or a path was deleted. `FileTreeNavigator` carries both to the activity, which tells the workspace, which tells the editor. Features still do not know each other (ADR-0010).
- When a path is deleted the editor closes its tabs under that path and drops their text without saving it, so a deleted file is not written back.

## Consequences

- A save of the deleted file that is already running may still recreate it. The window is a few milliseconds; a per-project saver (see ADR-0014) would remove it.
- Names that differ only in case count as different on this file system, as they do on Android.

## Open items

- Rename and move, with tabs following the file.
- Undo for delete (a trash folder inside `.asm`).
