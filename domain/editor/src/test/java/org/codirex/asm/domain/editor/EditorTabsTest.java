package org.codirex.asm.domain.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public final class EditorTabsTest {

  private static EditorTabs three() {
    return EditorTabs.empty().open("a.txt").open("b.txt").open("c.txt");
  }

  @Test
  public void startsEmpty() {
    EditorTabs tabs = EditorTabs.empty();
    assertTrue(tabs.paths().isEmpty());
    assertNull(tabs.activePath());
    assertFalse(tabs.isOpen("a.txt"));
  }

  @Test
  public void openingAddsAtTheEndAndActivates() {
    EditorTabs tabs = three();
    assertEquals(Arrays.asList("a.txt", "b.txt", "c.txt"), tabs.paths());
    assertEquals("c.txt", tabs.activePath());
  }

  @Test
  public void openingAnOpenFileOnlyActivatesIt() {
    EditorTabs tabs = three().open("a.txt");
    assertEquals(Arrays.asList("a.txt", "b.txt", "c.txt"), tabs.paths());
    assertEquals("a.txt", tabs.activePath());
  }

  @Test
  public void selectingChangesOnlyTheActiveFile() {
    EditorTabs tabs = three().select("b.txt");
    assertEquals("b.txt", tabs.activePath());
    assertEquals(3, tabs.paths().size());
  }

  @Test
  public void selectingAClosedFileOrTheActiveOneChangesNothing() {
    EditorTabs tabs = three();
    assertSame(tabs, tabs.select("missing.txt"));
    assertSame(tabs, tabs.select("c.txt"));
  }

  @Test
  public void closingTheActiveTabSelectsItsRightNeighbour() {
    EditorTabs tabs = three().select("b.txt").close("b.txt");
    assertEquals(Arrays.asList("a.txt", "c.txt"), tabs.paths());
    assertEquals("c.txt", tabs.activePath());
  }

  @Test
  public void closingTheLastActiveTabSelectsItsLeftNeighbour() {
    EditorTabs tabs = three().close("c.txt");
    assertEquals(Arrays.asList("a.txt", "b.txt"), tabs.paths());
    assertEquals("b.txt", tabs.activePath());
  }

  @Test
  public void closingAnInactiveTabKeepsTheActiveOne() {
    EditorTabs tabs = three().close("a.txt");
    assertEquals(Arrays.asList("b.txt", "c.txt"), tabs.paths());
    assertEquals("c.txt", tabs.activePath());
  }

  @Test
  public void closingTheOnlyTabLeavesNothingActive() {
    EditorTabs tabs = EditorTabs.empty().open("a.txt").close("a.txt");
    assertTrue(tabs.paths().isEmpty());
    assertNull(tabs.activePath());
  }

  @Test
  public void closingAFileThatIsNotOpenChangesNothing() {
    EditorTabs tabs = three();
    assertSame(tabs, tabs.close("missing.txt"));
  }

  @Test
  public void tabsAreImmutable() {
    EditorTabs before = three();
    before.close("a.txt");
    before.open("d.txt");
    assertEquals(Arrays.asList("a.txt", "b.txt", "c.txt"), before.paths());
    assertEquals("c.txt", before.activePath());
  }

  @Test
  public void equalTabsAreEqual() {
    assertEquals(three(), three());
    assertEquals(three().hashCode(), three().hashCode());
  }
}
