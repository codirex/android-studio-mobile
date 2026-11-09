# ADR-0015: Creating, renaming and deleting files and folders

Status: Proposed

## Context

The file tree only showed files. A project needs new files and folders, a way to rename them and a way to remove them. Moving to another folder is left for later because it needs a drag and drop or a folder picker.

## Decision

- The port `ProjectFiles` (domain:filetree) gets `create(project, parentPath, name, directory)`, `rename(project, path, newName)` and `delete(project, path)`. Failures keep using `ProjectException`: `ALREADY_EXISTS` when the name is taken, `STORAGE` for everything else, including an invalid name or a path outside the project.
- `EntryNames` in the domain decides which names are valid: not empty, at most 255 characters, not `.` or `..`, and without `/` or NUL. The dialog checks it before asking, and the data layer checks it again, because a name is a path segment and must never move the file somewhere else.
- `core:fs` gets `FileOperations.move(source, target)`: it fails if the source is missing, the target exists, or the target lies inside the source. It does not create parent folders.
- `data:project` resolves every path through `ProjectLocator`, so `..` and links cannot leave the project. The internal `.asm` folder cannot be created into, renamed, renamed to, deleted or replaced. The project root can be neither renamed nor deleted. A rename keeps the entry in its folder.
- The tree offers the actions on long press (folders: new file, new folder, rename, delete; files: rename, delete) and two buttons at the top for the project root, so an empty project can get its first file.
- Deleting asks for confirmation. Creating a file opens it in the editor.
- The tree view model reports what happened as a one-time event in its state (`TreeEvent`): a file was created and should be opened, a path was deleted, or a path was renamed. `FileTreeNavigator` carries them to the activity, which tells the workspace, which tells the editor. Features still do not know each other (ADR-0010).
- `FileTree.without(path)` drops a folder and everything under it from the tree, so a deleted or renamed folder is not listed again by the next refresh.
- When a path is deleted the editor closes its tabs under that path and drops their text without saving it, so a deleted file is not written back.
- When a path is renamed the editor remaps the paths of its tabs under it: the tab order, the active tab, the unsaved text and the unsaved marks follow. Unsaved text is then saved to the new name. A tab that was still loading is loaded again from the new path.

## Consequences

- A save that is already running for a deleted or renamed file may still recreate the old name. The window is a few milliseconds; a per-project saver (see ADR-0014) would remove it.
- Names that differ only in case count as different on this file system, as they do on Android.
- A renamed folder shows closed afterwards, because its loaded children are dropped.

## Open items

- Move to another folder.
- Undo for delete (a trash folder inside `.asm`).
