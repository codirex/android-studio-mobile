package org.codirex.asm.terminal.view;

import java.nio.charset.StandardCharsets;

/**
 * Turns keys and text into the bytes a program expects from a terminal (xterm conventions). Plain
 * Java, so it is tested without a device.
 */
public final class KeyEncoder {

  public static final int KEY_UP = 1;
  public static final int KEY_DOWN = 2;
  public static final int KEY_RIGHT = 3;
  public static final int KEY_LEFT = 4;
  public static final int KEY_HOME = 5;
  public static final int KEY_END = 6;
  public static final int KEY_PAGE_UP = 7;
  public static final int KEY_PAGE_DOWN = 8;
  public static final int KEY_INSERT = 9;
  public static final int KEY_DELETE = 10;
  public static final int KEY_TAB = 11;
  public static final int KEY_ENTER = 12;
  public static final int KEY_ESCAPE = 13;
  public static final int KEY_BACKSPACE = 14;
  /** F1 is {@code KEY_F1}, F12 is {@code KEY_F1 + 11}. */
  public static final int KEY_F1 = 20;

  private static final int ESC = 0x1b;
  private static final byte[] PASTE_START = {ESC, '[', '2', '0', '0', '~'};
  private static final byte[] PASTE_END = {ESC, '[', '2', '0', '1', '~'};
  private static final int[] FUNCTION_CODES = {15, 17, 18, 19, 20, 21, 23, 24};

  private KeyEncoder() {}

  /**
   * The bytes of a special key, or an empty array for a key this class does not know.
   *
   * @param applicationCursor the program asked for {@code ESC O x} cursor keys
   */
  public static byte[] key(
      int key, boolean ctrl, boolean alt, boolean shift, boolean applicationCursor) {
    int modifier = 1 + (shift ? 1 : 0) + (alt ? 2 : 0) + (ctrl ? 4 : 0);
    boolean modified = modifier > 1;
    switch (key) {
      case KEY_UP:
        return cursor('A', modified, modifier, applicationCursor);
      case KEY_DOWN:
        return cursor('B', modified, modifier, applicationCursor);
      case KEY_RIGHT:
        return cursor('C', modified, modifier, applicationCursor);
      case KEY_LEFT:
        return cursor('D', modified, modifier, applicationCursor);
      case KEY_HOME:
        return cursor('H', modified, modifier, applicationCursor);
      case KEY_END:
        return cursor('F', modified, modifier, applicationCursor);
      case KEY_PAGE_UP:
        return tilde(5, modified, modifier);
      case KEY_PAGE_DOWN:
        return tilde(6, modified, modifier);
      case KEY_INSERT:
        return tilde(2, modified, modifier);
      case KEY_DELETE:
        return tilde(3, modified, modifier);
      case KEY_TAB:
        return shift ? ascii("\u001b[Z") : withAlt(new byte[] {'\t'}, alt);
      case KEY_ENTER:
        return withAlt(new byte[] {'\r'}, alt);
      case KEY_ESCAPE:
        return new byte[] {ESC};
      case KEY_BACKSPACE:
        return withAlt(new byte[] {0x7f}, alt);
      default:
        break;
    }
    if (key >= KEY_F1 && key < KEY_F1 + 12) {
      int number = key - KEY_F1; // 0 for F1
      if (number < 4) {
        char letter = (char) ('P' + number);
        return modified
            ? ascii("\u001b[1;" + modifier + letter)
            : ascii("\u001bO" + letter);
      }
      return tilde(FUNCTION_CODES[number - 4], modified, modifier);
    }
    return new byte[0];
  }

  /**
   * The bytes of typed text. With {@code ctrl}, a single letter or one of {@code @[\]^_ ?} becomes
   * its control character; with {@code alt}, the text is preceded by ESC.
   */
  public static byte[] text(String text, boolean ctrl, boolean alt) {
    if (text.isEmpty()) {
      return new byte[0];
    }
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    if (ctrl && text.length() == 1) {
      int control = control(text.charAt(0));
      if (control >= 0) {
        bytes = new byte[] {(byte) control};
      }
    }
    return withAlt(bytes, alt);
  }

  /**
   * Pasted text: line breaks become carriage returns, as when typed. In bracketed mode the text is
   * wrapped so that the program knows it is a paste, and ESC characters are removed so that the
   * text cannot end the bracket early.
   */
  public static byte[] paste(String text, boolean bracketed) {
    String normalized = text.replace("\r\n", "\r").replace('\n', '\r');
    if (!bracketed) {
      return normalized.getBytes(StandardCharsets.UTF_8);
    }
    byte[] body = normalized.replace("\u001b", "").getBytes(StandardCharsets.UTF_8);
    byte[] out = new byte[PASTE_START.length + body.length + PASTE_END.length];
    System.arraycopy(PASTE_START, 0, out, 0, PASTE_START.length);
    System.arraycopy(body, 0, out, PASTE_START.length, body.length);
    System.arraycopy(PASTE_END, 0, out, PASTE_START.length + body.length, PASTE_END.length);
    return out;
  }

  private static int control(char c) {
    if (c >= 'a' && c <= 'z') {
      return c - 'a' + 1;
    }
    if (c >= 'A' && c <= 'Z') {
      return c - 'A' + 1;
    }
    if (c == ' ' || c == '@') {
      return 0;
    }
    if (c >= '[' && c <= '_') {
      return c - '@'; // [ \ ] ^ _ are 27 to 31
    }
    if (c == '?') {
      return 0x7f;
    }
    return -1;
  }

  private static byte[] cursor(char letter, boolean modified, int modifier, boolean application) {
    if (modified) {
      return ascii("\u001b[1;" + modifier + letter);
    }
    return ascii(application ? "\u001bO" + letter : "\u001b[" + letter);
  }

  private static byte[] tilde(int code, boolean modified, int modifier) {
    return ascii(modified ? "\u001b[" + code + ";" + modifier + "~" : "\u001b[" + code + "~");
  }

  private static byte[] withAlt(byte[] bytes, boolean alt) {
    if (!alt) {
      return bytes;
    }
    byte[] out = new byte[bytes.length + 1];
    out[0] = ESC;
    System.arraycopy(bytes, 0, out, 1, bytes.length);
    return out;
  }

  private static byte[] ascii(String text) {
    return text.getBytes(StandardCharsets.US_ASCII);
  }
}
