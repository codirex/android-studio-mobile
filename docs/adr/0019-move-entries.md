# ADR-0019: Moving files and folders

Status: Proposed

## Context

Renaming keeps an entry in its folder (ADR-0015). Reorganising a project also needs moving an entry to another folder. The screen is a phone: drag and drop in a tree is hard to do well there, and there is no clipboard model for files yet.

## Decision

- The port `ProjectFiles` gets `move(project, path, targetFolderPath)`, which puts the entry, with everything inside it, into the target folder under its own name, and `folders(project)`, which lists the paths of all folders of the project.
- `move` uses `FileOperations.move`, so it fails if the target exists or lies inside the source, and has the same guards as rename: `ALREADY_EXISTS` when the name is taken in the target folder; `STORAGE` for the project root, the internal `.asm` folder (as source or as target), a target that is not a folder, a missing source, and a path that leaves the project.
- `folders` walks the project, leaves out `.asm`, and stops at 32 levels or 5000 folders. That is far beyond a normal project and bounds the work for a damaged or linked tree.
- `MoveTargets` (domain, pure) decides what the user may choose: the project root and the listed folders, sorted ignoring case, without the folder the entry is already in (moving there changes nothing) and, for a folder, without itself and its subfolders.
- The tree offers it as "Move to..." in the long-press menu: the view model loads the folders in the background and the screen shows a list; choosing one moves the entry. The target folder opens and both folders are reloaded.
- Open editor tabs follow the entry through the same event as rename, because a move is a change of path (`EditorTabs.remapPath`). The language of a moved file is recomputed from its new path.

## Consequences

- Moving several entries at once, copy, and drag and drop are not part of this.
- A long list of folders is a plain list without search.
- Like rename, a moved folder shows closed in its new place, because what was loaded under the old path is dropped.

## Open items

- Copy and paste of entries.
- Multi-select.
