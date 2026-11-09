package org.codirex.asm.domain.filetree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public final class TreeNodeTest {

  @Test
  public void nameIsTheLastSegment() {
    assertEquals("Main.java", new TreeNode("app/src/Main.java", false).name());
    assertEquals("README.md", new TreeNode("README.md", false).name());
  }

  @Test
  public void parentPathIsEmptyAtTheRoot() {
    assertEquals("", new TreeNode("README.md", false).parentPath());
    assertEquals("app/src", new TreeNode("app/src/Main.java", false).parentPath());
  }

  @Test
  public void knowsWhetherItIsAFolder() {
    assertTrue(new TreeNode("src", true).isDirectory());
    assertFalse(new TreeNode("src", false).isDirectory());
  }

  @Test
  public void rejectsMalformedPaths() {
    String[] bad = {"", "/abs", "trailing/"};
    for (String path : bad) {
      try {
        new TreeNode(path, false);
        fail("expected a rejection of '" + path + "'");
      } catch (IllegalArgumentException expected) {
        // ok
      }
    }
  }

  @Test
  public void equalityUsesPathAndKind() {
    assertEquals(new TreeNode("a", true), new TreeNode("a", true));
    assertEquals(new TreeNode("a", true).hashCode(), new TreeNode("a", true).hashCode());
    assertNotEquals(new TreeNode("a", true), new TreeNode("a", false));
    assertNotEquals(new TreeNode("a", true), new TreeNode("b", true));
  }
}
