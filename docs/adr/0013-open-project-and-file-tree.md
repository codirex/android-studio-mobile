# ADR-0013: Opening a project and browsing its files

Status: Proposed

## Context

v1.0.0 needs to open a project, show its files as a tree and open text files. The tree must stay fast on large projects, must never block the main thread and must not let a path leave the project folder.

## Decision

- `domain:filetree` (pure JVM) holds the tree model and the port `ProjectFiles`. `TreeNode` identifies an entry by its path relative to the project root, with `/` as separator. `FileTree` is an immutable value: the folders loaded so far plus the set of expanded folders. `rows()` flattens it into the visible rows with their depth.
- Folders are loaded lazily, one level at a time, when the user expands them. A folder that is collapsed keeps its loaded children, so expanding it again is instant. Refreshing reloads the root and every loaded folder and replaces their children.
- `ProjectFiles.list(project, relativePath)` is implemented in `data:project` on top of `core:fs`, because that module already owns the projects root. Every path is resolved with `FileOperations.resolve`, so `..` and symbolic links cannot escape the project. The internal `.asm` folder is hidden. Folders come first, then files, each sorted by name ignoring case.
- `feature:filetree` shows the tree in a RecyclerView. The view model loads folders through Clotho (`runAsync`) and keeps a `FileTree` in its state. The project name arrives as a fragment argument through `SavedStateHandle`.
- The feature does not know the editor. It reports a tapped file through `FileTreeNavigator.openFile(project, relativePath)`, and `MainActivity` decides what happens (ADR-0010). Until the editor exists, the activity only shows a message.
- Opening a project from the list is `ProjectNavigator.showProject`, which `MainActivity` implements by showing the tree.

## Consequences

- The tree logic is tested on the JVM without Android.
- Large folders are listed whole in one background job. If this becomes slow, paging can be added behind the same port.
- File operations on the tree (create, rename, delete) and a file watcher are not part of this decision.

## Open items

- Whether to show hidden files (names starting with a dot) behind a setting.
- Context actions on rows: new file or folder, rename, delete.
