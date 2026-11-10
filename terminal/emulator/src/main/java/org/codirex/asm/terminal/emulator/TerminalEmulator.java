package org.codirex.asm.terminal.emulator;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A terminal emulator: it takes the bytes a program writes to its terminal, understands the
 * control characters and escape sequences in them (a large part of xterm / VT100), and keeps the
 * resulting screen, the scrollback and the cursor. It draws nothing and starts no process.
 *
 * <p>Not thread safe: one thread feeds it and reads it, or the caller synchronizes.
 *
 * <p>Supported: UTF-8, printable text with wide characters and automatic wrapping, cursor
 * movement, erasing, inserting and deleting lines and cells, scrolling and scroll regions, tab
 * stops, colors (16, 256 and true color) and text attributes, the alternate screen, saving the
 * cursor, the DEC line-drawing character set, window title (OSC 0 and 2) and the answers a program
 * asks for (cursor position, device attributes). Not supported yet: combining characters (they are
 * dropped), reflow of text when the width changes, origin mode, and the second character set.
 */
public final class TerminalEmulator {

  /** What the emulator tells its owner. All methods do nothing by default. */
  public interface Listener {
    /** Bytes the terminal answers to the program, to be written to the program's input. */
    default void write(byte[] reply) {}

    /** The program set the window title. */
    default void titleChanged(String title) {}

    /** The program rang the bell. */
    default void bell() {}
  }

  private enum State {
    GROUND,
    ESCAPE,
    CHARSET,
    CSI,
    OSC,
    IGNORED_STRING
  }

  private static final int MAX_PARAMS = 16;
  private static final int MAX_OSC_LENGTH = 4096;
  private static final int REPLACEMENT = 0xFFFD;

  private final int scrollbackLimit;
  private Listener listener = new Listener() {};

  private int rows;
  private int columns;
  private TerminalLine[] main;
  private TerminalLine[] alternate;
  private TerminalLine[] lines;
  private boolean alternateActive;
  private final TerminalLine[] scrollback;
  private int scrollbackStart;
  private int scrollbackSize;

  private int cursorRow;
  private int cursorColumn;
  private boolean wrapPending;
  private int foreground = TextStyle.DEFAULT_COLOR;
  private int background = TextStyle.DEFAULT_COLOR;
  private int attributes;
  private boolean graphicsCharset;

  private int scrollTop;
  private int scrollBottom;
  private boolean[] tabStops;

  private boolean autoWrap = true;
  private boolean insertMode;
  private boolean cursorVisible = true;
  private boolean applicationCursorKeys;
  private boolean bracketedPaste;
  private int mouseTracking;
  private boolean mouseSgr;
  private String title = "";

  private int savedRow;
  private int savedColumn;
  private int savedForeground = TextStyle.DEFAULT_COLOR;
  private int savedBackground = TextStyle.DEFAULT_COLOR;
  private int savedAttributes;
  private boolean savedWrapPending;
  private boolean savedGraphics;

  private State state = State.GROUND;
  private final int[] params = new int[MAX_PARAMS];
  private int paramCount;
  private boolean paramSeen;
  private int csiPrefix;
  private int csiIntermediate;
  private final StringBuilder osc = new StringBuilder();
  private boolean oscPending;
  private int charsetTarget;

  private int utf8Needed;
  private int utf8Pending;

  /**
   * @param rows number of rows, at least 1
   * @param columns number of columns, at least 1
   * @param scrollbackLines how many lines that scroll off the top are kept, 0 or more
   */
  public TerminalEmulator(int rows, int columns, int scrollbackLines) {
    if (rows < 1 || columns < 1 || scrollbackLines < 0) {
      throw new IllegalArgumentException("invalid size");
    }
    this.rows = rows;
    this.columns = columns;
    this.scrollbackLimit = scrollbackLines;
    this.scrollback = new TerminalLine[scrollbackLines];
    this.main = blankScreen(rows, columns);
    this.alternate = blankScreen(rows, columns);
    this.lines = main;
    this.scrollTop = 0;
    this.scrollBottom = rows - 1;
    resetTabStops();
  }

  public void setListener(Listener listener) {
    this.listener = listener == null ? new Listener() {} : listener;
  }

  // --- Input -----------------------------------------------------------------------------------

  /** Processes bytes the program wrote. A sequence may be split between calls. */
  public void append(byte[] data, int offset, int length) {
    for (int i = offset; i < offset + length; i++) {
      int b = data[i] & 0xff;
      if (utf8Needed > 0) {
        if ((b & 0xC0) == 0x80) {
          utf8Pending = (utf8Pending << 6) | (b & 0x3F);
          if (--utf8Needed == 0) {
            process(isValidScalar(utf8Pending) ? utf8Pending : REPLACEMENT);
          }
          continue;
        }
        utf8Needed = 0;
        process(REPLACEMENT);
      }
      if (b < 0x80) {
        process(b);
      } else if (b >= 0xC2 && b <= 0xDF) {
        utf8Needed = 1;
        utf8Pending = b & 0x1F;
      } else if (b >= 0xE0 && b <= 0xEF) {
        utf8Needed = 2;
        utf8Pending = b & 0x0F;
      } else if (b >= 0xF0 && b <= 0xF4) {
        utf8Needed = 3;
        utf8Pending = b & 0x07;
      } else {
        process(REPLACEMENT);
      }
    }
  }

  /** Processes text; it is encoded as UTF-8 first. */
  public void append(String text) {
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    append(bytes, 0, bytes.length);
  }

  private static boolean isValidScalar(int cp) {
    return cp <= 0x10FFFF && !(cp >= 0xD800 && cp <= 0xDFFF);
  }

  private void process(int cp) {
    // These control characters act in every state, even inside a sequence.
    if (cp == 0x18 || cp == 0x1A) {
      state = State.GROUND;
      oscPending = false;
      return;
    }
    if (cp == 0x1B) {
      // ESC inside an OSC string may start its terminator, ESC \.
      oscPending = state == State.OSC;
      state = State.ESCAPE;
      return;
    }
    switch (state) {
      case GROUND:
        ground(cp);
        break;
      case ESCAPE:
        escape(cp);
        break;
      case CHARSET:
        charset(cp);
        break;
      case CSI:
        csi(cp);
        break;
      case OSC:
        oscString(cp);
        break;
      case IGNORED_STRING:
        if (cp == 0x07) {
          state = State.GROUND;
        }
        break;
      default:
        break;
    }
  }

  private void ground(int cp) {
    if (cp < 0x20 || cp == 0x7F) {
      control(cp);
    } else if (cp >= 0x80 && cp < 0xA0) {
      // C1 controls are not used with UTF-8.
    } else {
      put(cp);
    }
  }

  private void control(int cp) {
    switch (cp) {
      case 0x07:
        listener.bell();
        break;
      case 0x08:
        moveTo(cursorRow, Math.max(0, cursorColumn - 1));
        break;
      case 0x09:
        moveTo(cursorRow, nextTabStop(cursorColumn));
        break;
      case 0x0A:
      case 0x0B:
      case 0x0C:
        lineFeed();
        break;
      case 0x0D:
        moveTo(cursorRow, 0);
        break;
      case 0x0E:
      case 0x0F:
        // Shift out and shift in: the second character set is not supported.
        break;
      default:
        break;
    }
  }

  private void escape(int cp) {
    boolean wasOscPending = oscPending;
    oscPending = false;
    state = State.GROUND;
    switch (cp) {
      case '[':
        startCsi();
        break;
      case ']':
        osc.setLength(0);
        oscPending = false;
        state = State.OSC;
        break;
      case 'P':
      case 'X':
      case '^':
      case '_':
        state = State.IGNORED_STRING;
        break;
      case '(':
        charsetTarget = 0;
        state = State.CHARSET;
        break;
      case ')':
      case '*':
      case '+':
        charsetTarget = 1;
        state = State.CHARSET;
        break;
      case '\\':
        if (wasOscPending) {
          finishOsc();
        }
        break;
      case '7':
        saveCursor();
        break;
      case '8':
        restoreCursor();
        break;
      case 'D':
        lineFeed();
        break;
      case 'E':
        moveTo(cursorRow, 0);
        lineFeed();
        break;
      case 'M':
        reverseIndex();
        break;
      case 'H':
        tabStops[cursorColumn] = true;
        break;
      case 'c':
        reset();
        break;
      default:
        // '=', '>' (keypad modes) and unknown sequences are ignored.
        break;
    }
  }

  private void charset(int cp) {
    state = State.GROUND;
    if (charsetTarget == 0) {
      graphicsCharset = cp == '0';
    }
  }

  private void startCsi() {
    state = State.CSI;
    Arrays.fill(params, 0);
    paramCount = 0;
    paramSeen = false;
    csiPrefix = 0;
    csiIntermediate = 0;
  }

  private void csi(int cp) {
    if (cp >= '0' && cp <= '9') {
      paramSeen = true;
      if (paramCount < MAX_PARAMS) {
        int value = params[paramCount] * 10 + (cp - '0');
        params[paramCount] = Math.min(value, 65535);
      }
    } else if (cp == ';' || cp == ':') {
      endParam();
    } else if (cp >= '<' && cp <= '?') {
      csiPrefix = cp;
    } else if (cp >= 0x20 && cp <= 0x2F) {
      csiIntermediate = cp;
    } else if (cp >= 0x40 && cp <= 0x7E) {
      endParam();
      state = State.GROUND;
      dispatchCsi(cp);
    } else if (cp < 0x20) {
      control(cp);
    } else {
      state = State.GROUND;
    }
  }

  private void endParam() {
    if (paramCount < MAX_PARAMS) {
      paramCount++;
    }
    paramSeen = false;
  }

  private void oscString(int cp) {
    if (cp == 0x07) {
      finishOsc();
      state = State.GROUND;
    } else if (cp >= 0x20) {
      if (osc.length() < MAX_OSC_LENGTH) {
        osc.appendCodePoint(cp);
      }
    }
  }

  private void finishOsc() {
    String text = osc.toString();
    osc.setLength(0);
    int semicolon = text.indexOf(';');
    if (semicolon < 0) {
      return;
    }
    String command = text.substring(0, semicolon);
    if (command.equals("0") || command.equals("2")) {
      title = text.substring(semicolon + 1);
      listener.titleChanged(title);
    }
  }

  // --- Escape sequences ------------------------------------------------------------------------

  /** The {@code index}-th parameter, or {@code fallback} when it is missing or zero. */
  private int param(int index, int fallback) {
    int count = paramCount;
    if (index >= count || index >= MAX_PARAMS) {
      return fallback;
    }
    int value = params[index];
    return value == 0 ? fallback : value;
  }

  private int paramRaw(int index) {
    return index < paramCount && index < MAX_PARAMS ? params[index] : 0;
  }

  private void dispatchCsi(int finalChar) {
    if (csiPrefix == '?') {
      if (finalChar == 'h' || finalChar == 'l') {
        for (int i = 0; i < Math.max(paramCount, 1); i++) {
          setPrivateMode(paramRaw(i), finalChar == 'h');
        }
      }
      return;
    }
    if (csiPrefix == '>') {
      if (finalChar == 'c') {
        reply("\u001b[>0;0;0c");
      }
      return;
    }
    if (csiPrefix != 0 || csiIntermediate != 0) {
      return;
    }
    switch (finalChar) {
      case 'A':
        moveTo(Math.max(scrollOrTop(), cursorRow - param(0, 1)), cursorColumn);
        break;
      case 'B':
        moveTo(Math.min(scrollOrBottom(), cursorRow + param(0, 1)), cursorColumn);
        break;
      case 'C':
      case 'a':
        moveTo(cursorRow, cursorColumn + param(0, 1));
        break;
      case 'D':
        moveTo(cursorRow, cursorColumn - param(0, 1));
        break;
      case 'E':
        moveTo(Math.min(scrollOrBottom(), cursorRow + param(0, 1)), 0);
        break;
      case 'F':
        moveTo(Math.max(scrollOrTop(), cursorRow - param(0, 1)), 0);
        break;
      case 'G':
      case '`':
        moveTo(cursorRow, param(0, 1) - 1);
        break;
      case 'H':
      case 'f':
        moveTo(param(0, 1) - 1, param(1, 1) - 1);
        break;
      case 'I':
        for (int i = 0; i < param(0, 1); i++) {
          moveTo(cursorRow, nextTabStop(cursorColumn));
        }
        break;
      case 'J':
        eraseInDisplay(paramRaw(0));
        break;
      case 'K':
        eraseInLine(paramRaw(0));
        break;
      case 'L':
        insertLines(param(0, 1));
        break;
      case 'M':
        deleteLines(param(0, 1));
        break;
      case 'P':
        deleteCells(param(0, 1));
        break;
      case '@':
        insertCells(param(0, 1));
        break;
      case 'X':
        eraseCells(param(0, 1));
        break;
      case 'S':
        scrollUp(param(0, 1));
        break;
      case 'T':
        scrollDown(param(0, 1));
        break;
      case 'd':
      case 'e':
        moveTo(param(0, 1) - 1, cursorColumn);
        break;
      case 'g':
        if (paramRaw(0) == 0) {
          tabStops[cursorColumn] = false;
        } else if (paramRaw(0) == 3) {
          Arrays.fill(tabStops, false);
        }
        break;
      case 'h':
      case 'l':
        for (int i = 0; i < Math.max(paramCount, 1); i++) {
          if (paramRaw(i) == 4) {
            insertMode = finalChar == 'h';
          }
        }
        break;
      case 'm':
        selectGraphicRendition();
        break;
      case 'n':
        deviceStatusReport(paramRaw(0));
        break;
      case 'c':
        if (paramRaw(0) == 0) {
          reply("\u001b[?1;2c");
        }
        break;
      case 'r':
        setScrollRegion(param(0, 1) - 1, param(1, rows) - 1);
        break;
      case 's':
        saveCursor();
        break;
      case 'u':
        restoreCursor();
        break;
      default:
        break;
    }
  }

  private int scrollOrTop() {
    return cursorRow >= scrollTop ? scrollTop : 0;
  }

  private int scrollOrBottom() {
    return cursorRow <= scrollBottom ? scrollBottom : rows - 1;
  }

  private void setPrivateMode(int mode, boolean on) {
    switch (mode) {
      case 1:
        applicationCursorKeys = on;
        break;
      case 7:
        autoWrap = on;
        if (!on) {
          wrapPending = false;
        }
        break;
      case 25:
        cursorVisible = on;
        break;
      case 47:
      case 1047:
        switchScreen(on, false);
        break;
      case 1049:
        switchScreen(on, true);
        break;
      case 1000:
      case 1002:
      case 1003:
        mouseTracking = on ? mode : 0;
        break;
      case 1006:
        mouseSgr = on;
        break;
      case 2004:
        bracketedPaste = on;
        break;
      default:
        break;
    }
  }

  private void deviceStatusReport(int kind) {
    if (kind == 5) {
      reply("\u001b[0n");
    } else if (kind == 6) {
      reply("\u001b[" + (cursorRow + 1) + ";" + (cursorColumn + 1) + "R");
    }
  }

  private void reply(String text) {
    listener.write(text.getBytes(StandardCharsets.US_ASCII));
  }

  private void selectGraphicRendition() {
    int count = Math.max(paramCount, 1);
    for (int i = 0; i < count; i++) {
      int code = paramRaw(i);
      switch (code) {
        case 0:
          foreground = TextStyle.DEFAULT_COLOR;
          background = TextStyle.DEFAULT_COLOR;
          attributes = 0;
          break;
        case 1:
          attributes |= TextStyle.BOLD;
          break;
        case 2:
          attributes |= TextStyle.FAINT;
          break;
        case 3:
          attributes |= TextStyle.ITALIC;
          break;
        case 4:
        case 21:
          attributes |= TextStyle.UNDERLINE;
          break;
        case 5:
        case 6:
          attributes |= TextStyle.BLINK;
          break;
        case 7:
          attributes |= TextStyle.INVERSE;
          break;
        case 8:
          attributes |= TextStyle.INVISIBLE;
          break;
        case 9:
          attributes |= TextStyle.STRIKETHROUGH;
          break;
        case 22:
          attributes &= ~(TextStyle.BOLD | TextStyle.FAINT);
          break;
        case 23:
          attributes &= ~TextStyle.ITALIC;
          break;
        case 24:
          attributes &= ~TextStyle.UNDERLINE;
          break;
        case 25:
          attributes &= ~TextStyle.BLINK;
          break;
        case 27:
          attributes &= ~TextStyle.INVERSE;
          break;
        case 28:
          attributes &= ~TextStyle.INVISIBLE;
          break;
        case 29:
          attributes &= ~TextStyle.STRIKETHROUGH;
          break;
        case 39:
          foreground = TextStyle.DEFAULT_COLOR;
          break;
        case 49:
          background = TextStyle.DEFAULT_COLOR;
          break;
        case 38:
        case 48:
          i = extendedColor(i, code == 38);
          break;
        default:
          if (code >= 30 && code <= 37) {
            foreground = code - 30;
          } else if (code >= 40 && code <= 47) {
            background = code - 40;
          } else if (code >= 90 && code <= 97) {
            foreground = code - 90 + 8;
          } else if (code >= 100 && code <= 107) {
            background = code - 100 + 8;
          }
          break;
      }
    }
  }

  /** Reads {@code 5;n} or {@code 2;r;g;b} after 38 or 48; returns the index of the last one used. */
  private int extendedColor(int index, boolean isForeground) {
    int mode = paramRaw(index + 1);
    int color;
    int last;
    if (mode == 5) {
      color = Math.min(paramRaw(index + 2), 255);
      last = index + 2;
    } else if (mode == 2) {
      color = TextStyle.rgb(paramRaw(index + 2), paramRaw(index + 3), paramRaw(index + 4));
      last = index + 4;
    } else {
      return index;
    }
    if (isForeground) {
      foreground = color;
    } else {
      background = color;
    }
    return last;
  }

  // --- Screen changes --------------------------------------------------------------------------

  private void put(int codePoint) {
    int cp = graphicsCharset ? graphic(codePoint) : codePoint;
    int width = CharWidth.of(cp);
    if (width == 0) {
      return;
    }
    if (wrapPending) {
      lines[cursorRow].wrapped = true;
      moveTo(cursorRow, 0);
      lineFeed();
      wrapPending = false;
    }
    if (width == 2 && cursorColumn == columns - 1) {
      if (!autoWrap) {
        return;
      }
      TerminalLine edge = lines[cursorRow];
      edge.clear(cursorColumn, columns, background);
      edge.wrapped = true;
      moveTo(cursorRow, 0);
      lineFeed();
    }
    TerminalLine line = lines[cursorRow];
    if (insertMode) {
      line.shiftRight(cursorColumn, width, background);
    }
    breakWideAt(line, cursorColumn);
    if (width == 2) {
      breakWideAt(line, cursorColumn + 1);
    }
    line.set(cursorColumn, cp, foreground, background, attributes);
    if (width == 2) {
      line.set(cursorColumn + 1, TerminalLine.CONTINUATION, foreground, background, attributes);
    }
    int next = cursorColumn + width;
    if (next >= columns) {
      cursorColumn = columns - 1;
      wrapPending = autoWrap;
    } else {
      cursorColumn = next;
    }
  }

  /** Before a cell is overwritten: a wide character that loses a half loses both. */
  private void breakWideAt(TerminalLine line, int column) {
    if (column >= columns) {
      return;
    }
    if (line.chars[column] == TerminalLine.CONTINUATION && column > 0) {
      line.chars[column - 1] = TerminalLine.BLANK;
    } else if (column + 1 < columns && line.chars[column + 1] == TerminalLine.CONTINUATION) {
      line.chars[column + 1] = TerminalLine.BLANK;
    }
  }

  private static int graphic(int cp) {
    switch (cp) {
      case '`': return 0x25C6;
      case 'a': return 0x2592;
      case 'f': return 0x00B0;
      case 'g': return 0x00B1;
      case 'j': return 0x2518;
      case 'k': return 0x2510;
      case 'l': return 0x250C;
      case 'm': return 0x2514;
      case 'n': return 0x253C;
      case 'o': return 0x23BA;
      case 'p': return 0x23BB;
      case 'q': return 0x2500;
      case 'r': return 0x23BC;
      case 's': return 0x23BD;
      case 't': return 0x251C;
      case 'u': return 0x2524;
      case 'v': return 0x2534;
      case 'w': return 0x252C;
      case 'x': return 0x2502;
      case 'y': return 0x2264;
      case 'z': return 0x2265;
      case '{': return 0x03C0;
      case '|': return 0x2260;
      case '}': return 0x00A3;
      case '~': return 0x00B7;
      default: return cp;
    }
  }

  private void moveTo(int row, int column) {
    cursorRow = Math.max(0, Math.min(rows - 1, row));
    cursorColumn = Math.max(0, Math.min(columns - 1, column));
    wrapPending = false;
  }

  private int nextTabStop(int from) {
    for (int i = from + 1; i < columns; i++) {
      if (tabStops[i]) {
        return i;
      }
    }
    return columns - 1;
  }

  private void resetTabStops() {
    tabStops = new boolean[columns];
    for (int i = 8; i < columns; i += 8) {
      tabStops[i] = true;
    }
  }

  private void lineFeed() {
    wrapPending = false;
    if (cursorRow == scrollBottom) {
      scrollUp(1);
    } else if (cursorRow < rows - 1) {
      cursorRow++;
    }
  }

  private void reverseIndex() {
    wrapPending = false;
    if (cursorRow == scrollTop) {
      scrollDown(1);
    } else if (cursorRow > 0) {
      cursorRow--;
    }
  }

  /** Scrolls the scroll region up; lines that leave the top of the whole screen are kept. */
  private void scrollUp(int count) {
    int height = scrollBottom - scrollTop + 1;
    int n = Math.min(count, height);
    for (int i = 0; i < n; i++) {
      TerminalLine removed = lines[scrollTop];
      if (!alternateActive && scrollTop == 0 && scrollBottom == rows - 1) {
        keepInScrollback(removed);
      }
      System.arraycopy(lines, scrollTop + 1, lines, scrollTop, height - 1);
      lines[scrollBottom] = new TerminalLine(columns, background);
    }
  }

  private void scrollDown(int count) {
    int height = scrollBottom - scrollTop + 1;
    int n = Math.min(count, height);
    for (int i = 0; i < n; i++) {
      System.arraycopy(lines, scrollTop, lines, scrollTop + 1, height - 1);
      lines[scrollTop] = new TerminalLine(columns, background);
    }
  }

  private void keepInScrollback(TerminalLine line) {
    if (scrollbackLimit == 0) {
      return;
    }
    if (scrollbackSize < scrollbackLimit) {
      scrollback[(scrollbackStart + scrollbackSize) % scrollbackLimit] = line;
      scrollbackSize++;
    } else {
      scrollback[scrollbackStart] = line;
      scrollbackStart = (scrollbackStart + 1) % scrollbackLimit;
    }
  }

  private void insertLines(int count) {
    if (cursorRow < scrollTop || cursorRow > scrollBottom) {
      return;
    }
    int height = scrollBottom - cursorRow + 1;
    int n = Math.min(count, height);
    for (int i = 0; i < n; i++) {
      System.arraycopy(lines, cursorRow, lines, cursorRow + 1, height - 1);
      lines[cursorRow] = new TerminalLine(columns, background);
    }
    moveTo(cursorRow, 0);
  }

  private void deleteLines(int count) {
    if (cursorRow < scrollTop || cursorRow > scrollBottom) {
      return;
    }
    int height = scrollBottom - cursorRow + 1;
    int n = Math.min(count, height);
    for (int i = 0; i < n; i++) {
      System.arraycopy(lines, cursorRow + 1, lines, cursorRow, height - 1);
      lines[scrollBottom] = new TerminalLine(columns, background);
    }
    moveTo(cursorRow, 0);
  }

  private void insertCells(int count) {
    TerminalLine line = lines[cursorRow];
    breakWideAt(line, cursorColumn);
    line.shiftRight(cursorColumn, count, background);
    wrapPending = false;
  }

  private void deleteCells(int count) {
    TerminalLine line = lines[cursorRow];
    breakWideAt(line, cursorColumn);
    line.shiftLeft(cursorColumn, count, background);
    wrapPending = false;
  }

  private void eraseCells(int count) {
    TerminalLine line = lines[cursorRow];
    int end = Math.min(columns, cursorColumn + count);
    breakWideAt(line, cursorColumn);
    if (end < columns) {
      breakWideAt(line, end);
    }
    line.clear(cursorColumn, end, background);
    wrapPending = false;
  }

  private void eraseInLine(int mode) {
    TerminalLine line = lines[cursorRow];
    if (mode == 0) {
      breakWideAt(line, cursorColumn);
      line.clear(cursorColumn, columns, background);
    } else if (mode == 1) {
      breakWideAt(line, cursorColumn);
      line.clear(0, cursorColumn + 1, background);
    } else if (mode == 2) {
      line.clear(0, columns, background);
    }
    if (mode == 0 || mode == 2) {
      line.wrapped = false;
    }
  }

  private void eraseInDisplay(int mode) {
    if (mode == 0) {
      eraseInLine(0);
      for (int r = cursorRow + 1; r < rows; r++) {
        lines[r].clear(0, columns, background);
        lines[r].wrapped = false;
      }
    } else if (mode == 1) {
      for (int r = 0; r < cursorRow; r++) {
        lines[r].clear(0, columns, background);
        lines[r].wrapped = false;
      }
      eraseInLine(1);
    } else if (mode == 2) {
      for (int r = 0; r < rows; r++) {
        lines[r].clear(0, columns, background);
        lines[r].wrapped = false;
      }
    } else if (mode == 3) {
      scrollbackStart = 0;
      scrollbackSize = 0;
      Arrays.fill(scrollback, null);
    }
  }

  private void setScrollRegion(int top, int bottom) {
    int t = Math.max(0, top);
    int b = Math.min(rows - 1, bottom);
    if (t < b) {
      scrollTop = t;
      scrollBottom = b;
      moveTo(0, 0);
    }
  }

  private void saveCursor() {
    savedRow = cursorRow;
    savedColumn = cursorColumn;
    savedForeground = foreground;
    savedBackground = background;
    savedAttributes = attributes;
    savedWrapPending = wrapPending;
    savedGraphics = graphicsCharset;
  }

  private void restoreCursor() {
    cursorRow = Math.min(savedRow, rows - 1);
    cursorColumn = Math.min(savedColumn, columns - 1);
    foreground = savedForeground;
    background = savedBackground;
    attributes = savedAttributes;
    wrapPending = savedWrapPending;
    graphicsCharset = savedGraphics;
  }

  private void switchScreen(boolean toAlternate, boolean saveCursorToo) {
    if (toAlternate == alternateActive) {
      return;
    }
    if (toAlternate) {
      if (saveCursorToo) {
        saveCursor();
      }
      alternateActive = true;
      lines = alternate;
      for (TerminalLine line : lines) {
        line.clear(0, columns, TextStyle.DEFAULT_COLOR);
        line.wrapped = false;
      }
    } else {
      alternateActive = false;
      lines = main;
      if (saveCursorToo) {
        restoreCursor();
      }
    }
    scrollTop = 0;
    scrollBottom = rows - 1;
    wrapPending = false;
  }

  /** The full reset (ESC c): the screen, the modes, the colors and the tab stops. */
  public void reset() {
    alternateActive = false;
    main = blankScreen(rows, columns);
    alternate = blankScreen(rows, columns);
    lines = main;
    scrollbackStart = 0;
    scrollbackSize = 0;
    Arrays.fill(scrollback, null);
    cursorRow = 0;
    cursorColumn = 0;
    wrapPending = false;
    foreground = TextStyle.DEFAULT_COLOR;
    background = TextStyle.DEFAULT_COLOR;
    attributes = 0;
    graphicsCharset = false;
    scrollTop = 0;
    scrollBottom = rows - 1;
    autoWrap = true;
    insertMode = false;
    cursorVisible = true;
    applicationCursorKeys = false;
    bracketedPaste = false;
    mouseTracking = 0;
    mouseSgr = false;
    saveCursor();
    resetTabStops();
    state = State.GROUND;
  }

  /**
   * Changes the size. Text is not reflowed: lines are cut or padded. When the screen gets shorter
   * the lines at the top move to the scrollback so the cursor line stays visible.
   */
  public void resize(int newRows, int newColumns) {
    if (newRows < 1 || newColumns < 1) {
      throw new IllegalArgumentException("invalid size");
    }
    if (newRows == rows && newColumns == columns) {
      return;
    }
    // Blank lines below the cursor are dropped first, then lines from the top.
    int dropped = 0;
    if (newRows < rows) {
      int fromBottom = Math.min(rows - newRows, rows - 1 - cursorRow);
      dropped = rows - newRows - fromBottom;
    }
    main = resizeScreen(main, newRows, newColumns, dropped, !alternateActive);
    alternate = resizeScreen(alternate, newRows, newColumns, dropped, false);
    lines = alternateActive ? alternate : main;
    for (int i = 0; i < scrollbackSize; i++) {
      scrollback[(scrollbackStart + i) % scrollbackLimit].resize(newColumns, TextStyle.DEFAULT_COLOR);
    }
    rows = newRows;
    columns = newColumns;
    cursorRow = Math.max(0, Math.min(cursorRow - dropped, rows - 1));
    cursorColumn = Math.min(cursorColumn, columns - 1);
    savedRow = Math.max(0, Math.min(savedRow - dropped, rows - 1));
    savedColumn = Math.min(savedColumn, columns - 1);
    wrapPending = false;
    scrollTop = 0;
    scrollBottom = rows - 1;
    resetTabStops();
  }

  private TerminalLine[] resizeScreen(
      TerminalLine[] screen, int newRows, int newColumns, int dropped, boolean keepDropped) {
    TerminalLine[] result = new TerminalLine[newRows];
    if (keepDropped) {
      for (int i = 0; i < dropped; i++) {
        keepInScrollback(screen[i]);
      }
    }
    int copied = Math.min(newRows, screen.length - dropped);
    for (int i = 0; i < copied; i++) {
      TerminalLine line = screen[dropped + i];
      line.resize(newColumns, TextStyle.DEFAULT_COLOR);
      result[i] = line;
    }
    for (int i = copied; i < newRows; i++) {
      result[i] = new TerminalLine(newColumns, TextStyle.DEFAULT_COLOR);
    }
    return result;
  }

  private static TerminalLine[] blankScreen(int rows, int columns) {
    TerminalLine[] screen = new TerminalLine[rows];
    for (int i = 0; i < rows; i++) {
      screen[i] = new TerminalLine(columns, TextStyle.DEFAULT_COLOR);
    }
    return screen;
  }

  // --- Reading the screen ----------------------------------------------------------------------

  public int rows() {
    return rows;
  }

  public int columns() {
    return columns;
  }

  /** How many lines of scrollback are kept now. */
  public int scrollbackSize() {
    return scrollbackSize;
  }

  /** The row of the cursor, 0 at the top of the screen. */
  public int cursorRow() {
    return cursorRow;
  }

  public int cursorColumn() {
    return cursorColumn;
  }

  public boolean isCursorVisible() {
    return cursorVisible;
  }

  public String title() {
    return title;
  }

  /** Whether the alternate screen (full-screen programs) is shown. */
  public boolean isAlternateScreen() {
    return alternateActive;
  }

  /** The cursor keys send {@code ESC O x} instead of {@code ESC [ x}. */
  public boolean isApplicationCursorKeys() {
    return applicationCursorKeys;
  }

  /** Pasted text must be wrapped in {@code ESC [ 200 ~} and {@code ESC [ 201 ~}. */
  public boolean isBracketedPaste() {
    return bracketedPaste;
  }

  /** 0 when the program does not want the mouse, otherwise 1000, 1002 or 1003. */
  public int mouseTracking() {
    return mouseTracking;
  }

  public boolean isMouseSgr() {
    return mouseSgr;
  }

  private TerminalLine lineAt(int row) {
    if (row >= 0 && row < rows) {
      return lines[row];
    }
    if (row < 0 && row >= -scrollbackSize && !alternateActive) {
      return scrollback[(scrollbackStart + scrollbackSize + row) % scrollbackLimit];
    }
    throw new IndexOutOfBoundsException("row " + row);
  }

  /**
   * The code point in a cell, or 0 for the right half of a wide character. {@code row} is 0 for the
   * top of the screen and negative for the scrollback: -1 is the line just above the screen.
   */
  public int codePointAt(int row, int column) {
    return lineAt(row).chars[column];
  }

  public int foregroundAt(int row, int column) {
    return lineAt(row).foregrounds[column];
  }

  public int backgroundAt(int row, int column) {
    return lineAt(row).backgrounds[column];
  }

  /** The {@link TextStyle} flags of a cell. */
  public int attributesAt(int row, int column) {
    return lineAt(row).attributes[column];
  }

  /** Whether the text of this row continues on the next one. */
  public boolean isWrapped(int row) {
    return lineAt(row).wrapped;
  }

  /** The text of a row without trailing blanks; rows are numbered as in {@link #codePointAt}. */
  public String lineText(int row) {
    return lineAt(row).text();
  }

  /** The text of the screen, rows joined with line breaks, without trailing blank rows. */
  public String screenText() {
    StringBuilder out = new StringBuilder();
    int last = rows - 1;
    while (last >= 0 && lines[last].text().isEmpty()) {
      last--;
    }
    for (int r = 0; r <= last; r++) {
      if (r > 0) {
        out.append('\n');
      }
      out.append(lines[r].text());
    }
    return out.toString();
  }
}
