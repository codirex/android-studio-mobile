package org.codirex.asm.domain.editor.highlight;

/** Small helpers the lexers share. */
final class Scan {
  private Scan() {}

  static boolean startsWith(CharSequence text, int at, String prefix) {
    int length = prefix.length();
    if (at < 0 || at + length > text.length()) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      if (text.charAt(at + i) != prefix.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  /** The index of {@code what} at or after {@code from}, or -1. */
  static int indexOf(CharSequence text, String what, int from) {
    int last = text.length() - what.length();
    for (int i = Math.max(from, 0); i <= last; i++) {
      if (startsWith(text, i, what)) {
        return i;
      }
    }
    return -1;
  }

  /** The index of the line break at or after {@code from}, or the length of the text. */
  static int lineEnd(CharSequence text, int from) {
    int n = text.length();
    for (int i = from; i < n; i++) {
      char c = text.charAt(i);
      if (c == '\n' || c == '\r') {
        return i;
      }
    }
    return n;
  }

  /**
   * The end of a string that starts with a quote at {@code start}: after the closing quote, or at
   * the end of the line when the string is not closed (so that a half typed string does not colour
   * the rest of the file).
   */
  static int stringEnd(CharSequence text, int start) {
    char quote = text.charAt(start);
    int n = text.length();
    int i = start + 1;
    while (i < n) {
      char c = text.charAt(i);
      if (c == '\\') {
        i += 2;
      } else if (c == quote) {
        return i + 1;
      } else if (c == '\n' || c == '\r') {
        return i;
      } else {
        i++;
      }
    }
    return n;
  }
}
