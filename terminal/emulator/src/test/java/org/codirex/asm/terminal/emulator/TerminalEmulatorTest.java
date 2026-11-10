package org.codirex.asm.terminal.emulator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public final class TerminalEmulatorTest {

  private static final String ESC = "\u001b";

  private static TerminalEmulator terminal(int rows, int columns) {
    return new TerminalEmulator(rows, columns, 100);
  }

  @Test
  public void plainTextIsWrittenFromTheTopLeft() {
    TerminalEmulator t = terminal(3, 10);
    t.append("hello");

    assertEquals("hello", t.lineText(0));
    assertEquals(5, t.cursorColumn());
    assertEquals(0, t.cursorRow());
  }

  @Test
  public void carriageReturnAndLineFeedStartANewLine() {
    TerminalEmulator t = terminal(3, 10);
    t.append("one\r\ntwo");

    assertEquals("one", t.lineText(0));
    assertEquals("two", t.lineText(1));
  }

  @Test
  public void aLineFeedAloneKeepsTheColumn() {
    TerminalEmulator t = terminal(3, 10);
    t.append("ab\ncd");

    assertEquals("ab", t.lineText(0));
    assertEquals("  cd", t.lineText(1));
  }

  @Test
  public void backspaceMovesLeftAndTheNextCharacterOverwrites() {
    TerminalEmulator t = terminal(2, 10);
    t.append("abc\bX");

    assertEquals("abX", t.lineText(0));
  }

  @Test
  public void textWrapsAtTheRightEdgeOnlyWhenTheNextCharacterComes() {
    TerminalEmulator t = terminal(3, 5);
    t.append("abcde");

    assertEquals("abcde", t.lineText(0));
    assertEquals(0, t.cursorRow());
    assertEquals(4, t.cursorColumn());

    t.append("f");

    assertEquals("f", t.lineText(1));
    assertTrue(t.isWrapped(0));
    assertEquals(1, t.cursorRow());
  }

  @Test
  public void withoutAutoWrapTheLastColumnIsOverwritten() {
    TerminalEmulator t = terminal(2, 5);
    t.append(ESC + "[?7l" + "abcdefg");

    assertEquals("abcdg", t.lineText(0));
    assertEquals("", t.lineText(1));
  }

  @Test
  public void scrollingKeepsTheLinesThatLeaveTheTop() {
    TerminalEmulator t = terminal(2, 5);
    t.append("1\r\n2\r\n3\r\n4");

    assertEquals("3", t.lineText(0));
    assertEquals("4", t.lineText(1));
    assertEquals(2, t.scrollbackSize());
    assertEquals("1", t.lineText(-2));
    assertEquals("2", t.lineText(-1));
  }

  @Test
  public void theScrollbackKeepsOnlyTheLastLines() {
    TerminalEmulator t = new TerminalEmulator(2, 5, 3);
    for (int i = 1; i <= 10; i++) {
      t.append(i + "\r\n");
    }

    assertEquals(3, t.scrollbackSize());
    assertEquals("7", t.lineText(-3));
    assertEquals("8", t.lineText(-2));
    assertEquals("9", t.lineText(-1));
  }

  @Test
  public void cursorPositionIsOneBasedAndClamped() {
    TerminalEmulator t = terminal(5, 10);
    t.append(ESC + "[3;4H" + "x");

    assertEquals("   x", t.lineText(2));

    t.append(ESC + "[99;99H");
    assertEquals(4, t.cursorRow());
    assertEquals(9, t.cursorColumn());

    t.append(ESC + "[H");
    assertEquals(0, t.cursorRow());
    assertEquals(0, t.cursorColumn());
  }

  @Test
  public void relativeCursorMovement() {
    TerminalEmulator t = terminal(5, 10);
    t.append(ESC + "[3;5H" + ESC + "[2A" + ESC + "[3C" + ESC + "[1B" + ESC + "[2D");

    assertEquals(1, t.cursorRow());
    assertEquals(5, t.cursorColumn());
  }

  @Test
  public void eraseInLineVariants() {
    TerminalEmulator t = terminal(4, 10);
    t.append("abcdef" + ESC + "[4G" + ESC + "[K");
    assertEquals("abc", t.lineText(0));

    t.append("\r\n" + "abcdef" + ESC + "[4G" + ESC + "[1K");
    assertEquals("    ef", t.lineText(1));

    t.append("\r\n" + "abcdef" + ESC + "[2K");
    assertEquals("", t.lineText(2));
  }

  @Test
  public void eraseInDisplayVariants() {
    TerminalEmulator t = terminal(3, 5);
    t.append("aaaaa\r\nbbbbb\r\nccccc" + ESC + "[2;3H" + ESC + "[J");

    assertEquals("aaaaa", t.lineText(0));
    assertEquals("bb", t.lineText(1));
    assertEquals("", t.lineText(2));

    t.append(ESC + "[2J");
    assertEquals("", t.screenText());
  }

  @Test
  public void colorsAndAttributesAreStoredPerCell() {
    TerminalEmulator t = terminal(2, 10);
    t.append(ESC + "[1;31;44mA" + ESC + "[0mB");

    assertEquals(1, t.foregroundAt(0, 0));
    assertEquals(4, t.backgroundAt(0, 0));
    assertEquals(TextStyle.BOLD, t.attributesAt(0, 0));
    assertEquals(TextStyle.DEFAULT_COLOR, t.foregroundAt(0, 1));
    assertEquals(0, t.attributesAt(0, 1));
  }

  @Test
  public void brightAnd256AndTrueColors() {
    TerminalEmulator t = terminal(2, 10);
    t.append(ESC + "[91mA" + ESC + "[38;5;200mB" + ESC + "[48;2;10;20;30mC");

    assertEquals(9, t.foregroundAt(0, 0));
    assertEquals(200, t.foregroundAt(0, 1));
    assertTrue(TextStyle.isTrueColor(t.backgroundAt(0, 2)));
    assertEquals(0x0a141e, TextStyle.rgbValue(t.backgroundAt(0, 2)));
    assertEquals(200, t.foregroundAt(0, 2));
  }

  @Test
  public void anSgrWithoutParametersResetsAndAttributesCanBeTurnedOff() {
    TerminalEmulator t = terminal(2, 10);
    t.append(ESC + "[1;4;7mA" + ESC + "[22;24mB" + ESC + "[mC");

    assertEquals(TextStyle.BOLD | TextStyle.UNDERLINE | TextStyle.INVERSE, t.attributesAt(0, 0));
    assertEquals(TextStyle.INVERSE, t.attributesAt(0, 1));
    assertEquals(0, t.attributesAt(0, 2));
  }

  @Test
  public void erasedCellsTakeTheCurrentBackground() {
    TerminalEmulator t = terminal(2, 6);
    t.append("abc" + ESC + "[42m" + ESC + "[K");

    assertEquals(2, t.backgroundAt(0, 4));
    assertEquals("abc", t.lineText(0));
  }

  @Test
  public void insertAndDeleteCharacters() {
    TerminalEmulator t = terminal(2, 8);
    t.append("abcdef" + ESC + "[3G" + ESC + "[2@");
    assertEquals("ab  cdef", t.lineText(0));

    t.append(ESC + "[3G" + ESC + "[2P");
    assertEquals("abcdef", t.lineText(0));
  }

  @Test
  public void eraseCharactersDoesNotMoveTheRest() {
    TerminalEmulator t = terminal(2, 8);
    t.append("abcdef" + ESC + "[2G" + ESC + "[3X");

    assertEquals("a   ef", t.lineText(0));
  }

  @Test
  public void insertAndDeleteLinesInsideTheScrollRegion() {
    TerminalEmulator t = terminal(4, 3);
    t.append("1\r\n2\r\n3\r\n4" + ESC + "[2;1H" + ESC + "[L");

    assertEquals("1", t.lineText(0));
    assertEquals("", t.lineText(1));
    assertEquals("2", t.lineText(2));
    assertEquals("3", t.lineText(3));

    t.append(ESC + "[M");
    assertEquals("2", t.lineText(1));
    assertEquals("3", t.lineText(2));
    assertEquals("", t.lineText(3));
  }

  @Test
  public void aScrollRegionScrollsOnlyItsRowsAndKeepsNothingInTheScrollback() {
    TerminalEmulator t = terminal(4, 3);
    t.append("a\r\nb\r\nc\r\nd" + ESC + "[2;3r" + ESC + "[3;1H" + "\n");

    assertEquals("a", t.lineText(0));
    assertEquals("c", t.lineText(1));
    assertEquals("", t.lineText(2));
    assertEquals("d", t.lineText(3));
    assertEquals(0, t.scrollbackSize());
  }

  @Test
  public void reverseIndexAtTheTopScrollsDown() {
    TerminalEmulator t = terminal(3, 3);
    t.append("a\r\nb\r\nc" + ESC + "[H" + ESC + "M");

    assertEquals("", t.lineText(0));
    assertEquals("a", t.lineText(1));
    assertEquals("b", t.lineText(2));
  }

  @Test
  public void tabsGoToTheNextMultipleOfEight() {
    TerminalEmulator t = terminal(2, 20);
    t.append("a\tb");

    assertEquals("a       b", t.lineText(0));
  }

  @Test
  public void saveAndRestoreTheCursorAndItsColors() {
    TerminalEmulator t = terminal(4, 10);
    t.append(ESC + "[2;3H" + ESC + "[31m" + ESC + "7" + ESC + "[H" + ESC + "[0m" + ESC + "8" + "X");

    assertEquals("  X", t.lineText(1));
    assertEquals(1, t.foregroundAt(1, 2));
  }

  @Test
  public void theAlternateScreenKeepsTheMainScreenSafe() {
    TerminalEmulator t = terminal(3, 10);
    t.append("main" + ESC + "[?1049h" + "full screen" + ESC + "[?1049l");

    assertFalse(t.isAlternateScreen());
    assertEquals("main", t.lineText(0));
    assertEquals(4, t.cursorColumn());
    assertEquals(0, t.scrollbackSize());
  }

  @Test
  public void theAlternateScreenStartsBlankAndDoesNotFillTheScrollback() {
    TerminalEmulator t = terminal(2, 5);
    t.append(ESC + "[?1049h" + "a\r\nb\r\nc");

    assertTrue(t.isAlternateScreen());
    assertEquals("b", t.lineText(0));
    assertEquals(0, t.scrollbackSize());
  }

  @Test
  public void modesAreReported() {
    TerminalEmulator t = terminal(2, 5);
    t.append(ESC + "[?25l" + ESC + "[?1h" + ESC + "[?2004h" + ESC + "[?1000h" + ESC + "[?1006h");

    assertFalse(t.isCursorVisible());
    assertTrue(t.isApplicationCursorKeys());
    assertTrue(t.isBracketedPaste());
    assertEquals(1000, t.mouseTracking());
    assertTrue(t.isMouseSgr());
  }

  @Test
  public void theTitleIsSetByOscEndedWithBellOrStringTerminator() {
    TerminalEmulator t = terminal(2, 5);
    final String[] seen = new String[2];
    t.setListener(
        new TerminalEmulator.Listener() {
          int count;

          @Override
          public void titleChanged(String title) {
            seen[count++] = title;
          }
        });

    t.append(ESC + "]0;first\u0007" + ESC + "]2;second" + ESC + "\\" + "x");

    assertEquals("first", seen[0]);
    assertEquals("second", seen[1]);
    assertEquals("second", t.title());
    assertEquals("x", t.lineText(0));
  }

  @Test
  public void aProgramCanAskForTheCursorPositionAndDeviceAttributes() {
    TerminalEmulator t = terminal(5, 10);
    final ByteArrayOutputStream replies = new ByteArrayOutputStream();
    t.setListener(
        new TerminalEmulator.Listener() {
          @Override
          public void write(byte[] reply) {
            replies.write(reply, 0, reply.length);
          }
        });

    t.append(ESC + "[3;4H" + ESC + "[6n" + ESC + "[c");

    assertEquals(ESC + "[3;4R" + ESC + "[?1;2c", new String(replies.toByteArray(), StandardCharsets.US_ASCII));
  }

  @Test
  public void theBellIsReported() {
    TerminalEmulator t = terminal(2, 5);
    final int[] bells = new int[1];
    t.setListener(
        new TerminalEmulator.Listener() {
          @Override
          public void bell() {
            bells[0]++;
          }
        });

    t.append("a\u0007b\u0007");

    assertEquals(2, bells[0]);
    assertEquals("ab", t.lineText(0));
  }

  @Test
  public void utf8IsDecodedEvenWhenSplitBetweenCalls() {
    TerminalEmulator t = terminal(2, 10);
    byte[] bytes = "é€😀".getBytes(StandardCharsets.UTF_8);
    for (byte b : bytes) {
      t.append(new byte[] {b}, 0, 1);
    }

    assertEquals("é€😀", t.lineText(0));
  }

  @Test
  public void invalidUtf8GivesTheReplacementCharacterAndRecovers() {
    TerminalEmulator t = terminal(2, 10);
    t.append(new byte[] {(byte) 0xff, 'a', (byte) 0xe2, (byte) 0x82, 'b'}, 0, 5);

    assertEquals("�a�b", t.lineText(0));
  }

  @Test
  public void aSequenceSplitBetweenCallsStillWorks() {
    TerminalEmulator t = terminal(2, 10);
    t.append("a" + ESC);
    t.append("[3");
    t.append("1mb");

    assertEquals("ab", t.lineText(0));
    assertEquals(1, t.foregroundAt(0, 1));
  }

  @Test
  public void aWideCharacterTakesTwoColumns() {
    TerminalEmulator t = terminal(2, 6);
    t.append("a漢b");

    assertEquals("a漢b", t.lineText(0));
    assertEquals('漢', t.codePointAt(0, 1));
    assertEquals(0, t.codePointAt(0, 2));
    assertEquals('b', t.codePointAt(0, 3));
    assertEquals(4, t.cursorColumn());
  }

  @Test
  public void aWideCharacterThatDoesNotFitMovesToTheNextLine() {
    TerminalEmulator t = terminal(3, 5);
    t.append("abcd漢");

    assertEquals("abcd", t.lineText(0));
    assertEquals("漢", t.lineText(1));
  }

  @Test
  public void overwritingHalfOfAWideCharacterRemovesTheWholeCharacter() {
    TerminalEmulator t = terminal(2, 6);
    t.append("漢" + ESC + "[2G" + "x");

    assertEquals(' ', t.codePointAt(0, 0));
    assertEquals('x', t.codePointAt(0, 1));
  }

  @Test
  public void combiningCharactersAreDroppedForNow() {
    TerminalEmulator t = terminal(2, 6);
    t.append("éx");

    assertEquals("ex", t.lineText(0));
  }

  @Test
  public void theLineDrawingCharacterSetMapsLettersToBoxCharacters() {
    TerminalEmulator t = terminal(2, 10);
    t.append(ESC + "(0lqk" + ESC + "(Bq");

    assertEquals("┌─┐q", t.lineText(0));
  }

  @Test
  public void unknownSequencesAreIgnoredAndDoNotBreakTheText() {
    TerminalEmulator t = terminal(2, 10);
    t.append("a" + ESC + "[?9999h" + ESC + "[>5u" + ESC + "=" + ESC + "Pdevice" + ESC + "\\" + "b");

    assertEquals("ab", t.lineText(0));
  }

  @Test
  public void cancelStopsASequence() {
    TerminalEmulator t = terminal(2, 10);
    t.append(ESC + "[31\u0018x");

    assertEquals("x", t.lineText(0));
    assertEquals(TextStyle.DEFAULT_COLOR, t.foregroundAt(0, 0));
  }

  @Test
  public void aFullResetClearsEverything() {
    TerminalEmulator t = terminal(2, 5);
    t.append(ESC + "[31m" + "a\r\nb\r\nc" + ESC + "c");

    assertEquals("", t.screenText());
    assertEquals(0, t.scrollbackSize());
    assertEquals(0, t.cursorRow());
    t.append("x");
    assertEquals(TextStyle.DEFAULT_COLOR, t.foregroundAt(0, 0));
  }

  @Test
  public void growingTheScreenAddsBlankSpace() {
    TerminalEmulator t = terminal(2, 5);
    t.append("ab\r\ncd");
    t.resize(4, 8);

    assertEquals(4, t.rows());
    assertEquals(8, t.columns());
    assertEquals("ab", t.lineText(0));
    assertEquals("cd", t.lineText(1));
    assertEquals(1, t.cursorRow());
  }

  @Test
  public void shrinkingKeepsTheCursorLineAndMovesTheTopToTheScrollback() {
    TerminalEmulator t = terminal(4, 5);
    t.append("1\r\n2\r\n3\r\n4");
    t.resize(2, 5);

    assertEquals("3", t.lineText(0));
    assertEquals("4", t.lineText(1));
    assertEquals(2, t.scrollbackSize());
    assertEquals("1", t.lineText(-2));
    assertEquals(1, t.cursorRow());
  }

  @Test
  public void shrinkingDropsBlankLinesBelowTheCursorFirst() {
    TerminalEmulator t = terminal(5, 5);
    t.append("1\r\n2");
    t.resize(3, 5);

    assertEquals("1", t.lineText(0));
    assertEquals("2", t.lineText(1));
    assertEquals(0, t.scrollbackSize());
  }

  @Test
  public void narrowingCutsTheLinesAndKeepsTheCursorInside() {
    TerminalEmulator t = terminal(2, 10);
    t.append("abcdefghij");
    t.resize(2, 4);

    assertEquals("abcd", t.lineText(0));
    assertTrue(t.cursorColumn() <= 3);
  }

  @Test
  public void narrowingThroughAWideCharacterLeavesNoHalfOfIt() {
    TerminalEmulator t = terminal(2, 6);
    t.append("abc漢");
    t.resize(2, 4);

    assertEquals("abc", t.lineText(0));
  }

  @Test
  public void aVeryLongSequenceOfParametersDoesNotBreakAnything() {
    TerminalEmulator t = terminal(2, 10);
    StringBuilder sequence = new StringBuilder(ESC + "[");
    for (int i = 0; i < 100; i++) {
      sequence.append("1;");
    }
    sequence.append("mx");
    t.append(sequence.toString());

    assertEquals("x", t.lineText(0));
  }

  @Test
  public void aVeryLongOscStringIsCapped() {
    TerminalEmulator t = terminal(2, 10);
    StringBuilder text = new StringBuilder(ESC + "]0;");
    for (int i = 0; i < 20000; i++) {
      text.append('t');
    }
    text.append("\u0007y");
    t.append(text.toString());

    assertEquals("y", t.lineText(0));
    assertTrue(t.title().length() <= 4096);
  }

  @Test
  public void aCursorBelowTheRegionIsNotAffectedByMarginsOnMove() {
    TerminalEmulator t = terminal(6, 5);
    t.append(ESC + "[2;4r" + ESC + "[6;1H" + ESC + "[3A");

    assertEquals(2, t.cursorRow());
  }
}
