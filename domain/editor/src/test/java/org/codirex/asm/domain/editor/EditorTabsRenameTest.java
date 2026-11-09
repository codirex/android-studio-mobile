package org.codirex.asm.domain.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import org.junit.Test;

public final class EditorTabsRenameTest {

  @Test
  public void remapsTheExactPathAndPathsInsideAFolder() {
    assertEquals("b.txt", EditorTabs.remapPath("a.txt", "a.txt", "b.txt"));
    assertEquals("lib/App.java", EditorTabs.remapPath("src/App.java", "src", "lib"));
    assertEquals("lib/x/App.java", EditorTabs.remapPath("src/x/App.java", "src", "lib"));
  }

  @Test
  public void leavesOtherPathsAlone() {
    assertEquals("a.txt.bak", EditorTabs.remapPath("a.txt.bak", "a.txt", "b.txt"));
    assertEquals("src2/App.java", EditorTabs.remapPath("src2/App.java", "src", "lib"));
    assertEquals("other/src", EditorTabs.remapPath("other/src", "src", "lib"));
  }

  @Test
  public void renamingAFileKeepsTheOrderAndTheActiveTab() {
    EditorTabs tabs = EditorTabs.empty().open("a.txt").open("b.txt").open("c.txt").select("b.txt");

    EditorTabs renamed = tabs.renamed("b.txt", "z.txt");

    assertEquals(Arrays.asList("a.txt", "z.txt", "c.txt"), renamed.paths());
    assertEquals("z.txt", renamed.activePath());
  }

  @Test
  public void renamingAFolderMovesEveryTabInsideIt() {
    EditorTabs tabs =
        EditorTabs.empty().open("src/a.txt").open("readme.md").open("src/b/c.txt").select("readme.md");

    EditorTabs renamed = tabs.renamed("src", "lib");

    assertEquals(Arrays.asList("lib/a.txt", "readme.md", "lib/b/c.txt"), renamed.paths());
    assertEquals("readme.md", renamed.activePath());
  }

  @Test
  public void renamingWithNothingOpenOrNoMatchChangesNothing() {
    assertNull(EditorTabs.empty().renamed("a", "b").activePath());

    EditorTabs tabs = EditorTabs.empty().open("a.txt");
    assertEquals(tabs, tabs.renamed("other", "x"));
  }
}
