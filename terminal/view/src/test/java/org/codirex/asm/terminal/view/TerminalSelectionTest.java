package org.codirex.asm.terminal.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.codirex.asm.terminal.emulator.TerminalEmulator;
import org.junit.Test;

public final class TerminalSelectionTest {

  private final TerminalEmulator emulator = new TerminalEmulator(5, 10, 50);
  private final TerminalSelection selection = new TerminalSelection();

  @Test
  public void inactiveSelectionIsEmpty() {
    assertFalse(selection.isActive());
    assertFalse(selection.contains(0, 0));
    assertEquals("", selection.text(emulator));
  }

  @Test
  public void selectsPartOfARow() {
    emulator.append("hello world");
    selection.start(0, 1);
    selection.extend(0, 3);
    assertEquals("ell", selection.text(emulator));
    assertTrue(selection.contains(0, 2));
    assertFalse(selection.contains(0, 4));
  }

  @Test
  public void selectionWorksBackwards() {
    emulator.append("abcdef");
    selection.start(0, 4);
    selection.extend(0, 1);
    assertEquals("bcde", selection.text(emulator));
  }

  @Test
  public void rowsAreJoinedWithLineBreaksAndTrimmed() {
    emulator.append("one\r\ntwo\r\nthree");
    selection.start(0, 0);
    selection.extend(2, 9);
    assertEquals("one\ntwo\nthree", selection.text(emulator));
  }

  @Test
  public void wrappedRowsAreJoinedWithoutBreak() {
    emulator.append("0123456789ABC");
    selection.start(0, 0);
    selection.extend(1, 9);
    assertEquals("0123456789ABC", selection.text(emulator));
  }

  @Test
  public void containsSpansRows() {
    selection.start(0, 5);
    selection.extend(2, 2);
    assertTrue(selection.contains(0, 9));
    assertTrue(selection.contains(1, 0));
    assertTrue(selection.contains(2, 2));
    assertFalse(selection.contains(2, 3));
    assertFalse(selection.contains(0, 4));
  }

  @Test
  public void clearEndsTheSelection() {
    emulator.append("abc");
    selection.start(0, 0);
    selection.clear();
    assertFalse(selection.isActive());
    assertEquals("", selection.text(emulator));
  }

  @Test
  public void scrollbackRowsCanBeSelected() {
    emulator.append("a\r\nb\r\nc\r\nd\r\ne\r\nf\r\ng");
    selection.start(-2, 0);
    selection.extend(0, 0);
    String text = selection.text(emulator);
    assertTrue(text, text.startsWith("a") || text.startsWith("b") || text.startsWith("c"));
    assertTrue(text.endsWith("c"));
  }
}
