# ADR-0020: Our own terminal emulator

Status: Proposed

## Context

ADR-0006 left open whether to reuse the Termux terminal libraries or write our own emulator, and said the licence of the libraries had to be checked first.

What we found: the `termux/termux-app` repository is released under GPLv3 only. Its `LICENSE.md` names one exception: code taken from Terminal Emulator for Android (Apache 2.0), and it points at the `terminal-view` and `terminal-emulator` libraries. So those libraries are a mix of Apache-2.0 code and GPLv3 code written for Termux, and the files that are which are not marked. This project is Apache-2.0. Depending on those libraries could force us to GPLv3, and we cannot settle that by reading a file; it would need a legal opinion.

## Decision

- We write our own emulator. It is a clean implementation from the public xterm and VT100 behaviour; no Termux code is read or copied.
- It is split so that the part that matters most is testable without a device:
  - `terminal:emulator` (pure JVM, no Android): turns the bytes a program writes into a screen: cells with a code point, colors and attributes, the scrollback, the cursor, the modes, the title. It draws nothing and starts no process. This ADR covers it.
  - A view that draws an emulator's screen and turns touches and keys into bytes (Android, later).
  - Pseudo-terminal and process handling (native code and the service of ADR-0006, later).
- The emulator is single-threaded by contract and reports to a `Listener`: bytes to send back to the program (replies to cursor-position and device-attribute requests), title changes and the bell.
- Reading the screen is by position: `codePointAt`, `foregroundAt`, `backgroundAt`, `attributesAt`, `isWrapped`, with negative rows for the scrollback. Colors are an int: default, a palette index or a true color. The view maps them to real colors, so themes do not touch the emulator.
- Supported now: UTF-8 (also split across calls, with a replacement character for bad bytes), wide characters, automatic wrapping that waits for the next character, cursor movement and saving, erase, insert and delete of cells and lines, scroll regions, tab stops, 16, 256 and true colors and the usual attributes, the alternate screen, the DEC line-drawing set, OSC 0 and 2 for the title, status replies, application cursor keys, bracketed paste and mouse modes (reported, so the view can act on them), resize and full reset.
- Not supported yet: combining characters (dropped), reflow of lines when the width changes (lines are cut or padded), origin mode, the second character set (SO and SI), sixel and other graphics, hyperlinks (OSC 8), the clipboard (OSC 52).
- Input is bounded: at most 16 parameters in a sequence, parameters up to 65535, titles up to 4096 characters. A broken or hostile stream cannot make it allocate without limit.
- Where the emulator lives in the processes (open): ADR-0003 allows `feature` modules to use only `terminal:protocol`. The simplest design is that the service owns the pseudo-terminal and a bounded buffer of raw output, and the UI process runs the emulator on that stream, replaying the buffer when it reattaches. That needs `feature:terminal` to depend on `terminal:emulator`, which means amending ADR-0003 rule 5 and `tools/check_layering.py`; this is decided together with the service design, not here.

## Consequences

- We own the correctness of terminal behaviour. The tests encode what programs such as shells, `less`, `vim` and `ncurses` applications expect, and grow with every bug.
- No licence risk from the emulator itself.
- A first version will not be as complete as a mature one. The unsupported list above is the backlog.

## Open items

- The view (drawing, selection, keyboard, soft keys for Ctrl, Esc and Tab, mouse, scrolling).
- The service, the pseudo-terminal (JNI) and the output buffer between the processes.
- Key encoding for function keys and modifiers.
- Reflow when the width changes, combining characters, OSC 8 and OSC 52.
- Testing against recorded output of real programs.
