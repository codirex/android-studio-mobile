package org.codirex.asm.terminal.view;

import org.codirex.asm.terminal.emulator.TerminalEmulator;

/**
 * A text selection on the screen, from one cell to another, read the way text runs: along the
 * rows, from the start cell to the end cell. Rows are numbered as in the emulator (0 is the top
 * of the screen, negative rows are the scrollback). Not thread safe; the owner holds the screen
 * lock when it reads text.
 */
final class TerminalSelection {

  private boolean active;
  private int anchorRow;
  private int anchorColumn;
  private int endRow;
  private int endColumn;

  boolean isActive() {
    return active;
  }

  /** Starts a selection of one cell. */
  void start(int row, int column) {
    active = true;
    anchorRow = row;
    anchorColumn = column;
    endRow = row;
    endColumn = column;
  }

  /** Moves the free end of the selection. */
  void extend(int row, int column) {
    if (active) {
      endRow = row;
      endColumn = column;
    }
  }

  void clear() {
    active = false;
  }

  private boolean anchorFirst() {
    return anchorRow < endRow || (anchorRow == endRow && anchorColumn <= endColumn);
  }

  /** Whether the cell is selected. */
  boolean contains(int row, int column) {
    if (!active) {
      return false;
    }
    int firstRow = anchorFirst() ? anchorRow : endRow;
    int firstColumn = anchorFirst() ? anchorColumn : endColumn;
    int lastRow = anchorFirst() ? endRow : anchorRow;
    int lastColumn = anchorFirst() ? endColumn : anchorColumn;
    if (row < firstRow || row > lastRow) {
      return false;
    }
    if (row == firstRow && column < firstColumn) {
      return false;
    }
    return !(row == lastRow && column > lastColumn);
  }

  /**
   * The selected text: trailing blanks of each row are dropped, and rows that were only wrapped by
   * the screen width are joined without a line break.
   */
  String text(TerminalEmulator emulator) {
    if (!active) {
      return "";
    }
    int firstRow = anchorFirst() ? anchorRow : endRow;
    int firstColumn = anchorFirst() ? anchorColumn : endColumn;
    int lastRow = anchorFirst() ? endRow : anchorRow;
    int lastColumn = anchorFirst() ? endColumn : anchorColumn;
    int minRow = emulator.isAlternateScreen() ? 0 : -emulator.scrollbackSize();
    firstRow = Math.max(firstRow, minRow);
    lastRow = Math.min(lastRow, emulator.rows() - 1);
    StringBuilder out = new StringBuilder();
    for (int row = firstRow; row <= lastRow; row++) {
      int from = row == firstRow ? firstColumn : 0;
      int to = row == lastRow ? lastColumn : emulator.columns() - 1;
      to = Math.min(to, emulator.columns() - 1);
      StringBuilder line = new StringBuilder();
      for (int column = Math.max(0, from); column <= to; column++) {
        int codePoint = emulator.codePointAt(row, column);
        if (codePoint == 0) {
          continue; // the right half of a wide character
        }
        line.appendCodePoint(codePoint);
      }
      int end = line.length();
      while (end > 0 && line.charAt(end - 1) == ' ') {
        end--;
      }
      out.append(line, 0, end);
      boolean joined = emulator.isWrapped(row) && to == emulator.columns() - 1;
      if (row < lastRow && !joined) {
        out.append('\n');
      }
    }
    return out.toString();
  }
}
