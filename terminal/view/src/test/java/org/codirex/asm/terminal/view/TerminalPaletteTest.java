package org.codirex.asm.terminal.view;

import static org.junit.Assert.assertEquals;

import org.codirex.asm.terminal.emulator.TextStyle;
import org.junit.Test;

public final class TerminalPaletteTest {

  @Test
  public void theDefaultColorIsWhatTheCallerSays() {
    assertEquals(0xff112233, TerminalPalette.argb(TextStyle.DEFAULT_COLOR, 0xff112233));
  }

  @Test
  public void namedColors() {
    assertEquals(0xff000000, TerminalPalette.argb(0, 0));
    assertEquals(0xffcd3131, TerminalPalette.argb(1, 0));
    assertEquals(0xffffffff, TerminalPalette.argb(15, 0));
  }

  @Test
  public void theColorCube() {
    assertEquals(0xff000000, TerminalPalette.argb(16, 0));
    assertEquals(0xffff0000, TerminalPalette.argb(196, 0)); // 16 + 36*5
    assertEquals(0xff00ff00, TerminalPalette.argb(46, 0)); // 16 + 6*5
    assertEquals(0xff0000ff, TerminalPalette.argb(21, 0)); // 16 + 5
    assertEquals(0xffffffff, TerminalPalette.argb(231, 0));
    assertEquals(0xff5f87af, TerminalPalette.argb(16 + 36 * 1 + 6 * 2 + 3, 0));
  }

  @Test
  public void theGrays() {
    assertEquals(0xff080808, TerminalPalette.argb(232, 0));
    assertEquals(0xffeeeeee, TerminalPalette.argb(255, 0));
  }

  @Test
  public void trueColor() {
    assertEquals(0xff123456, TerminalPalette.argb(TextStyle.rgb(0x12, 0x34, 0x56), 0));
  }
}
