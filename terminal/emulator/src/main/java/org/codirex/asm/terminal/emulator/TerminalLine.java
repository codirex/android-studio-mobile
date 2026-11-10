package org.codirex.asm.terminal.emulator;

import java.util.Arrays;

/** One row of cells. A cell is a code point, a foreground, a background and attribute flags. */
final class TerminalLine {

  /** The code point of an empty cell. */
  static final int BLANK = ' ';

  /** The code point of the right half of a wide character. */
  static final int CONTINUATION = 0;

  int[] chars;
  int[] foregrounds;
  int[] backgrounds;
  int[] attributes;

  /** Whether the text continues on the next row because it did not fit. */
  boolean wrapped;

  TerminalLine(int columns, int background) {
    chars = new int[columns];
    foregrounds = new int[columns];
    backgrounds = new int[columns];
    attributes = new int[columns];
    clear(0, columns, background);
    wrapped = false;
  }

  int columns() {
    return chars.length;
  }

  /** Blanks the cells from {@code from} (included) to {@code to} (excluded). */
  void clear(int from, int to, int background) {
    int end = Math.min(to, chars.length);
    for (int i = Math.max(from, 0); i < end; i++) {
      chars[i] = BLANK;
      foregrounds[i] = TextStyle.DEFAULT_COLOR;
      backgrounds[i] = background;
      attributes[i] = 0;
    }
  }

  void set(int column, int codePoint, int foreground, int background, int attribute) {
    chars[column] = codePoint;
    foregrounds[column] = foreground;
    backgrounds[column] = background;
    attributes[column] = attribute;
  }

  /** Moves the cells from {@code from} to the right by {@code count}; what falls off is lost. */
  void shiftRight(int from, int count, int background) {
    int length = chars.length;
    int moved = Math.min(count, length - from);
    if (moved <= 0) {
      return;
    }
    int copy = length - from - moved;
    if (copy > 0) {
      System.arraycopy(chars, from, chars, from + moved, copy);
      System.arraycopy(foregrounds, from, foregrounds, from + moved, copy);
      System.arraycopy(backgrounds, from, backgrounds, from + moved, copy);
      System.arraycopy(attributes, from, attributes, from + moved, copy);
    }
    clear(from, from + moved, background);
  }

  /** Removes {@code count} cells at {@code from}; the cells to the right move left. */
  void shiftLeft(int from, int count, int background) {
    int length = chars.length;
    int removed = Math.min(count, length - from);
    if (removed <= 0) {
      return;
    }
    int copy = length - from - removed;
    if (copy > 0) {
      System.arraycopy(chars, from + removed, chars, from, copy);
      System.arraycopy(foregrounds, from + removed, foregrounds, from, copy);
      System.arraycopy(backgrounds, from + removed, backgrounds, from, copy);
      System.arraycopy(attributes, from + removed, attributes, from, copy);
    }
    clear(length - removed, length, background);
  }

  /** Changes the number of columns; new cells are blank, cut cells are lost. */
  void resize(int columns, int background) {
    int old = chars.length;
    if (columns == old) {
      return;
    }
    // A wide character whose right half is cut off must not stay behind as half of one.
    boolean cutsWide = columns < old && columns > 0 && chars[columns] == CONTINUATION;
    chars = Arrays.copyOf(chars, columns);
    foregrounds = Arrays.copyOf(foregrounds, columns);
    backgrounds = Arrays.copyOf(backgrounds, columns);
    attributes = Arrays.copyOf(attributes, columns);
    if (columns > old) {
      clear(old, columns, background);
    } else if (cutsWide) {
      chars[columns - 1] = BLANK;
    }
  }

  /** The text of the row without trailing blanks. Wide characters count once. */
  String text() {
    StringBuilder out = new StringBuilder(chars.length);
    for (int c : chars) {
      if (c != CONTINUATION) {
        out.appendCodePoint(c);
      }
    }
    int end = out.length();
    while (end > 0 && out.charAt(end - 1) == ' ') {
      end--;
    }
    out.setLength(end);
    return out.toString();
  }
}
