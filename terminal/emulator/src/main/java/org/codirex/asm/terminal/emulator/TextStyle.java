package org.codirex.asm.terminal.emulator;

/**
 * How a cell is drawn. Colors are ints: {@link #DEFAULT_COLOR}, a palette index 0 to 255, or a true
 * color made with {@link #rgb}. Attributes are bit flags.
 */
public final class TextStyle {

  private TextStyle() {}

  /** The terminal's own foreground or background color. */
  public static final int DEFAULT_COLOR = -1;

  private static final int TRUE_COLOR = 0x01000000;

  public static final int BOLD = 1;
  public static final int FAINT = 1 << 1;
  public static final int ITALIC = 1 << 2;
  public static final int UNDERLINE = 1 << 3;
  public static final int BLINK = 1 << 4;
  public static final int INVERSE = 1 << 5;
  public static final int INVISIBLE = 1 << 6;
  public static final int STRIKETHROUGH = 1 << 7;

  /** A true color, 8 bits per channel. */
  public static int rgb(int red, int green, int blue) {
    return TRUE_COLOR | ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff);
  }

  public static boolean isTrueColor(int color) {
    return color >= 0 && (color & TRUE_COLOR) != 0;
  }

  public static boolean isPalette(int color) {
    return color >= 0 && color < 256;
  }

  /** The 0xRRGGBB value of a true color. */
  public static int rgbValue(int color) {
    return color & 0xffffff;
  }
}
