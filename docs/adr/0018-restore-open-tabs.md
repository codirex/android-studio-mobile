# ADR-0018: Restoring the open tabs

Status: Proposed

## Context

A developer closes the app, or Android ends its process, and expects to find the project as it was left. ADR-0014 listed the restoring of open tabs as an open item.

## Decision

- The open tabs and the active tab are part of the project, not of the app: they are written to `.asm/workspace.properties` inside the project folder, next to the project metadata. A project that is exported or copied carries its tabs, and deleting the project deletes them.
- `domain:editor` gets the port `WorkspaceStore` (`load` and `save` of an `EditorTabs`) and `EditorTabs.restore(paths, active)`, which drops empty and repeated paths and falls back to the last tab when the active one is not in the list.
- `data:project` writes the file as properties (`tabs`, `tab.N`, `active`) with the atomic write of `core:fs`. Reading never fails because of the content: a missing or damaged file gives no tabs.
- The editor view model restores the tabs when it is created, so the screen does not change. A restored file that no longer exists, is too large or is not text is dropped without a message, because the user did not just ask to open it.
- Every change of the tab list is saved: opening, selecting, closing, deleting and renaming. Saves never overlap: while one runs, only the newest list waits behind it. A failed save is ignored; the next change saves again.
- Unsaved text is not part of this. It is saved to the file by auto-save (ADR-0014), so the tabs show the file as it was saved.

## Consequences

- The scroll position, the cursor and the undo history are not restored.
- The file is rewritten on every tab change. It is a few lines, so the cost is small.
- The tab list is read once, when the editor of a project is created. Changing the file by hand while the project is open has no effect.

## Open items

- Restore the cursor and scroll position.
- Remember which project was open when the app was closed.
