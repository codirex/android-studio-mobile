package org.codirex.asm.terminal.view;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public final class KeyEncoderTest {

  private static byte[] s(String text) {
    return text.getBytes(StandardCharsets.ISO_8859_1);
  }

  private static byte[] key(int key) {
    return KeyEncoder.key(key, false, false, false, false);
  }

  @Test
  public void cursorKeysFollowTheApplicationMode() {
    assertArrayEquals(s("\u001b[A"), key(KeyEncoder.KEY_UP));
    assertArrayEquals(s("\u001b[D"), key(KeyEncoder.KEY_LEFT));
    assertArrayEquals(
        s("\u001bOA"), KeyEncoder.key(KeyEncoder.KEY_UP, false, false, false, true));
    assertArrayEquals(
        s("\u001bOH"), KeyEncoder.key(KeyEncoder.KEY_HOME, false, false, false, true));
    assertArrayEquals(s("\u001b[F"), key(KeyEncoder.KEY_END));
  }

  @Test
  public void modifiersAreEncodedInTheParameter() {
    assertArrayEquals(
        s("\u001b[1;5C"), KeyEncoder.key(KeyEncoder.KEY_RIGHT, true, false, false, false));
    assertArrayEquals(
        s("\u001b[1;2A"), KeyEncoder.key(KeyEncoder.KEY_UP, false, false, true, false));
    assertArrayEquals(
        s("\u001b[1;3D"), KeyEncoder.key(KeyEncoder.KEY_LEFT, false, true, false, true));
    assertArrayEquals(
        s("\u001b[3;5~"), KeyEncoder.key(KeyEncoder.KEY_DELETE, true, false, false, false));
  }

  @Test
  public void editingKeys() {
    assertArrayEquals(s("\u001b[5~"), key(KeyEncoder.KEY_PAGE_UP));
    assertArrayEquals(s("\u001b[6~"), key(KeyEncoder.KEY_PAGE_DOWN));
    assertArrayEquals(s("\u001b[2~"), key(KeyEncoder.KEY_INSERT));
    assertArrayEquals(s("\u001b[3~"), key(KeyEncoder.KEY_DELETE));
  }

  @Test
  public void tabEnterEscapeAndBackspace() {
    assertArrayEquals(s("\t"), key(KeyEncoder.KEY_TAB));
    assertArrayEquals(s("\u001b[Z"), KeyEncoder.key(KeyEncoder.KEY_TAB, false, false, true, false));
    assertArrayEquals(s("\r"), key(KeyEncoder.KEY_ENTER));
    assertArrayEquals(s("\u001b\r"), KeyEncoder.key(KeyEncoder.KEY_ENTER, false, true, false, false));
    assertArrayEquals(s("\u001b"), key(KeyEncoder.KEY_ESCAPE));
    assertArrayEquals(new byte[] {0x7f}, key(KeyEncoder.KEY_BACKSPACE));
  }

  @Test
  public void functionKeys() {
    assertArrayEquals(s("\u001bOP"), key(KeyEncoder.KEY_F1));
    assertArrayEquals(s("\u001bOS"), key(KeyEncoder.KEY_F1 + 3));
    assertArrayEquals(s("\u001b[15~"), key(KeyEncoder.KEY_F1 + 4));
    assertArrayEquals(s("\u001b[24~"), key(KeyEncoder.KEY_F1 + 11));
    assertArrayEquals(
        s("\u001b[1;5Q"), KeyEncoder.key(KeyEncoder.KEY_F1 + 1, true, false, false, false));
    assertArrayEquals(
        s("\u001b[20;2~"), KeyEncoder.key(KeyEncoder.KEY_F1 + 8, false, false, true, false));
  }

  @Test
  public void anUnknownKeyGivesNothing() {
    assertEquals(0, key(999).length);
  }

  @Test
  public void controlLetters() {
    assertArrayEquals(new byte[] {3}, KeyEncoder.text("c", true, false));
    assertArrayEquals(new byte[] {3}, KeyEncoder.text("C", true, false));
    assertArrayEquals(new byte[] {26}, KeyEncoder.text("z", true, false));
    assertArrayEquals(new byte[] {0}, KeyEncoder.text(" ", true, false));
    assertArrayEquals(new byte[] {27}, KeyEncoder.text("[", true, false));
    assertArrayEquals(new byte[] {28}, KeyEncoder.text("\\", true, false));
    assertArrayEquals(new byte[] {31}, KeyEncoder.text("_", true, false));
    assertArrayEquals(new byte[] {0x7f}, KeyEncoder.text("?", true, false));
  }

  @Test
  public void controlDoesNotChangeOtherText() {
    assertArrayEquals(s("5"), KeyEncoder.text("5", true, false));
    assertArrayEquals(s("ls"), KeyEncoder.text("ls", true, false));
  }

  @Test
  public void altPrefixesEscape() {
    assertArrayEquals(s("\u001bf"), KeyEncoder.text("f", false, true));
    assertArrayEquals(new byte[] {27, 3}, KeyEncoder.text("c", true, true));
  }

  @Test
  public void textIsUtf8() {
    assertArrayEquals("عربي".getBytes(StandardCharsets.UTF_8), KeyEncoder.text("عربي", false, false));
    assertEquals(0, KeyEncoder.text("", true, true).length);
  }

  @Test
  public void pastedLineBreaksBecomeCarriageReturns() {
    assertArrayEquals(s("a\rb\rc"), KeyEncoder.paste("a\nb\r\nc", false));
  }

  @Test
  public void bracketedPasteIsWrappedAndCannotEscapeTheBracket() {
    assertArrayEquals(
        s("\u001b[200~ls\r\u001b[201~"), KeyEncoder.paste("ls\n", true));
    byte[] hostile = KeyEncoder.paste("a\u001b[201~rm -rf", true);
    String text = new String(hostile, StandardCharsets.ISO_8859_1);
    // The only end marker is the one at the very end.
    assertEquals(text.length() - 6, text.indexOf("\u001b[201~"));
  }
}
