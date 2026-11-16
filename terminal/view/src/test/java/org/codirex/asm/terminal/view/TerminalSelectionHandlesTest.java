package org.codirex.asm.terminal.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TerminalSelectionHandlesTest {

  @Test
  public void movingTheAnchorChangesTheSelectedCells() {
    TerminalSelection selection = new TerminalSelection();
    selection.start(2, 5);
    selection.extend(2, 9);
    selection.moveAnchor(2, 7);
    assertFalse(selection.contains(2, 6));
    assertTrue(selection.contains(2, 7));
    assertTrue(selection.contains(2, 9));
    assertFalse(selection.contains(2, 10));
    assertEquals(7, selection.anchorColumn());
    assertEquals(9, selection.endColumn());
  }

  @Test
  public void draggingTheAnchorPastTheEndSwapsTheOrder() {
    TerminalSelection selection = new TerminalSelection();
    selection.start(1, 3);
    selection.extend(1, 6);
    assertTrue(selection.isAnchorFirst());
    selection.moveAnchor(1, 8);
    assertFalse(selection.isAnchorFirst());
    assertTrue(selection.contains(1, 6));
    assertTrue(selection.contains(1, 8));
    assertFalse(selection.contains(1, 5));
    assertFalse(selection.contains(1, 9));
  }

  @Test
  public void theAnchorCanMoveToAnotherRow() {
    TerminalSelection selection = new TerminalSelection();
    selection.start(0, 10);
    selection.extend(2, 3);
    assertTrue(selection.isAnchorFirst());
    selection.moveAnchor(3, 0);
    assertFalse(selection.isAnchorFirst());
    assertTrue(selection.contains(3, 0));
    assertTrue(selection.contains(2, 3));
    assertFalse(selection.contains(1, 0) && !selection.contains(2, 0));
  }

  @Test
  public void movingTheAnchorOfNoSelectionDoesNothing() {
    TerminalSelection selection = new TerminalSelection();
    selection.moveAnchor(1, 1);
    assertFalse(selection.isActive());
    assertFalse(selection.contains(1, 1));
  }
}
