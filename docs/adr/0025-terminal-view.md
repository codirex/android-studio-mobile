# ADR-0025: The terminal view

Status: Proposed

## Context

The terminal service ([ADR-0024](0024-terminal-service.md)) owns the shells and streams their output. The emulator ([ADR-0020](0020-terminal-emulator.md)) turns bytes into a screen but draws nothing. Something in the UI process has to connect the two, draw the screen, and turn keys, the soft keyboard and gestures into input. ADR-0003 lets a feature depend on `terminal:protocol` only, which is not enough for a feature that shows a terminal.

## Decision

- A new module `terminal:view` holds the UI side of the terminal: `TerminalConnection` (starts the service and binds to it), `TerminalView` (an Android `View`), `TerminalScreen` (an emulator fed from the output stream, with the offset logic of ADR-0024), `KeyEncoder` (keys and text to bytes, xterm conventions), `TerminalPalette` (emulator colors to ARGB), `TerminalSelection` (a text selection and the text it covers) and `TextScale` (the text size while pinching). It depends on `terminal:emulator`, `terminal:protocol` and `terminal:service` (only for the service's intent).
- **Rule change.** The table of ADR-0003 is widened by this record: a feature module may depend on `terminal:protocol` and on `terminal:view`, and on no other terminal module. `tools/check_layering.py` enforces it (`FEATURE_TERMINAL_MODULES`). Everything else in ADR-0003 stands.
- `TerminalScreen`, `KeyEncoder`, `TerminalPalette`, `TerminalSelection` and `TextScale` are plain Java and are tested on the JVM. `TerminalView` and `TerminalConnection` are thin Android code over them.
- `TerminalView` draws cell by cell with a monospace font (colors, bold, italic, underline, strikethrough, inverse, faint, the cursor), shows scrollback with a drag, sizes the terminal to the view (columns and rows from the text size) and tells the service when that changes.
- Input: hardware keys and the soft keyboard. The soft keyboard is configured as plain text without suggestions, so that characters arrive as typed and nothing is composed. Ctrl and Alt can be latched for the next key, which is what a row of extra keys needs. Paste is bracketed when the program asked for it, and escape characters are removed from pasted text.
- Selection and copy: a long press starts a selection, dragging extends it, and lifting the finger copies the text to the clipboard (rows that were only wrapped by the screen width are joined, trailing blanks are dropped). A tap clears the highlight. Pasting from the clipboard is a method of the view, which the extra keys row calls.
- Zoom: a two finger pinch changes the text size between 8 and 32 sp in half steps; rows and columns follow, and the service is told.
- All calls to the service go through one background thread, so the main thread never waits for another process and input keeps its order.
- A view shows one session. Attaching builds a new screen and replays what the service kept; detaching leaves the session running.

## Consequences

- `feature:terminal` has the sessions, the tabs, the extra keys row and the install screen, and uses the view as a component.
- The view draws every cell with its own call. That is simple and correct for wide and non-Latin characters; runs of equal style can be drawn together later if profiling asks for it.
- Not done yet: selection handles and word selection, remembering the text size, the mouse (the emulator reports the mode), a fling for scrolling, and right-to-left text. They are separate steps.

## Open items

- How the app reacts to the service process dying: sessions are lost, and the UI shows that and offers a new one.
- Whether the extra keys row belongs to `terminal:view` or to the feature (decided for now: the feature, with the latch methods of the view as its interface).
