package org.codirex.asm.terminal.view;

import org.codirex.asm.terminal.emulator.TextStyle;

/** Turns the colors of the emulator into ARGB values: the 16 named colors, the cube and the grays. */
public final class TerminalPalette {

  private static final int[] ANSI = {
    0x000000, 0xcd3131, 0x0dbc79, 0xe5e510, 0x2472c8, 0xbc3fbc, 0x11a8cd, 0xe5e5e5,
    0x666666, 0xf14c4c, 0x23d18b, 0xf5f543, 0x3b8eea, 0xd670d6, 0x29b8db, 0xffffff
  };

  private static final int[] TABLE = build();

  private TerminalPalette() {}

  private static int[] build() {
    int[] table = new int[256];
    System.arraycopy(ANSI, 0, table, 0, 16);
    int[] steps = {0, 95, 135, 175, 215, 255};
    for (int i = 0; i < 216; i++) {
      table[16 + i] = (steps[i / 36] << 16) | (steps[(i / 6) % 6] << 8) | steps[i % 6];
    }
    for (int i = 0; i < 24; i++) {
      int level = 8 + 10 * i;
      table[232 + i] = (level << 16) | (level << 8) | level;
    }
    return table;
  }

  /**
   * The opaque ARGB color for a color of the emulator.
   *
   * @param defaultColor what {@link TextStyle#DEFAULT_COLOR} stands for
   */
  public static int argb(int color, int defaultColor) {
    if (color == TextStyle.DEFAULT_COLOR) {
      return defaultColor;
    }
    if (TextStyle.isTrueColor(color)) {
      return 0xff000000 | TextStyle.rgbValue(color);
    }
    if (TextStyle.isPalette(color)) {
      return 0xff000000 | TABLE[color];
    }
    return defaultColor;
  }
}
