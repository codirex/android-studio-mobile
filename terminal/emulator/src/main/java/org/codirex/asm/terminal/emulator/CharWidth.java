package org.codirex.asm.terminal.emulator;

/**
 * How many terminal columns a character takes: 0 for combining and format characters, 2 for wide
 * East Asian characters and emoji, 1 for everything else. A compact table, not the full Unicode
 * data; it covers the common ranges.
 */
final class CharWidth {

  private CharWidth() {}

  private static final int[][] ZERO = {
    {0x0300, 0x036F}, {0x0483, 0x0489}, {0x0591, 0x05BD}, {0x0610, 0x061A}, {0x064B, 0x065F},
    {0x0670, 0x0670}, {0x06D6, 0x06DC}, {0x06DF, 0x06E4}, {0x1AB0, 0x1AFF}, {0x1DC0, 0x1DFF},
    {0x200B, 0x200F}, {0x2028, 0x202E}, {0x2060, 0x2064}, {0x20D0, 0x20FF}, {0xFE00, 0xFE0F},
    {0xFE20, 0xFE2F}, {0xFEFF, 0xFEFF}, {0xE0100, 0xE01EF}
  };

  private static final int[][] WIDE = {
    {0x1100, 0x115F}, {0x2E80, 0x303E}, {0x3041, 0x33FF}, {0x3400, 0x4DBF}, {0x4E00, 0x9FFF},
    {0xA000, 0xA4CF}, {0xAC00, 0xD7A3}, {0xF900, 0xFAFF}, {0xFE30, 0xFE6F}, {0xFF00, 0xFF60},
    {0xFFE0, 0xFFE6}, {0x1F300, 0x1F64F}, {0x1F680, 0x1F6FF}, {0x1F900, 0x1F9FF},
    {0x20000, 0x3FFFD}
  };

  static int of(int codePoint) {
    if (codePoint < 0x300) {
      return 1;
    }
    if (inTable(ZERO, codePoint)) {
      return 0;
    }
    return inTable(WIDE, codePoint) ? 2 : 1;
  }

  private static boolean inTable(int[][] table, int codePoint) {
    for (int[] range : table) {
      if (codePoint < range[0]) {
        return false;
      }
      if (codePoint <= range[1]) {
        return true;
      }
    }
    return false;
  }
}
