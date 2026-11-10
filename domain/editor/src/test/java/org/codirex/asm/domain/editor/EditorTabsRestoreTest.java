package org.codirex.asm.domain.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public final class EditorTabsRestoreTest {

  @Test
  public void keepsThePathsAndTheActiveTab() {
    EditorTabs tabs = EditorTabs.restore(Arrays.asList("a.txt", "b.txt", "c.txt"), "b.txt");

    assertEquals(Arrays.asList("a.txt", "b.txt", "c.txt"), tabs.paths());
    assertEquals("b.txt", tabs.activePath());
  }

  @Test
  public void dropsEmptyNullAndRepeatedPaths() {
    EditorTabs tabs = EditorTabs.restore(Arrays.asList("a.txt", "", null, "a.txt", "b.txt"), "a.txt");

    assertEquals(Arrays.asList("a.txt", "b.txt"), tabs.paths());
  }

  @Test
  public void anActiveTabThatIsNotOpenFallsBackToTheLastTab() {
    assertEquals(
        "b.txt", EditorTabs.restore(Arrays.asList("a.txt", "b.txt"), "gone.txt").activePath());
    assertEquals("b.txt", EditorTabs.restore(Arrays.asList("a.txt", "b.txt"), null).activePath());
  }

  @Test
  public void nothingToRestoreIsEmpty() {
    assertEquals(EditorTabs.empty(), EditorTabs.restore(Collections.<String>emptyList(), "a"));
    assertEquals(EditorTabs.empty(), EditorTabs.restore(Arrays.asList("", null), null));
    assertNull(EditorTabs.restore(Collections.<String>emptyList(), null).activePath());
  }

  @Test
  public void restoredTabsBehaveLikeOthers() {
    EditorTabs tabs = EditorTabs.restore(Arrays.asList("a.txt", "b.txt"), "a.txt").close("a.txt");

    assertEquals(Arrays.asList("b.txt"), tabs.paths());
    assertEquals("b.txt", tabs.activePath());
  }
}
