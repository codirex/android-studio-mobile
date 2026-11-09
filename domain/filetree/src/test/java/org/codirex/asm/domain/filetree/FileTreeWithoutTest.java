package org.codirex.asm.domain.filetree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public final class FileTreeWithoutTest {

  private static FileTree sample() {
    return FileTree.empty()
        .withChildren(
            "",
            Arrays.asList(
                new TreeNode("src", true), new TreeNode("src2", true), new TreeNode("a.txt", false)))
        .withChildren("src", Arrays.asList(new TreeNode("src/main", true)))
        .withChildren("src/main", Arrays.asList(new TreeNode("src/main/App.java", false)))
        .withChildren("src2", Collections.<TreeNode>emptyList())
        .withExpanded("src")
        .withExpanded("src/main")
        .withExpanded("src2");
  }

  @Test
  public void forgetsTheFolderAndEverythingUnderIt() {
    FileTree tree = sample().without("src");

    assertFalse(tree.isLoaded("src"));
    assertFalse(tree.isLoaded("src/main"));
    assertFalse(tree.isExpanded("src"));
    assertFalse(tree.isExpanded("src/main"));
  }

  @Test
  public void keepsFoldersWhoseNameOnlyStartsTheSame() {
    FileTree tree = sample().without("src");

    assertTrue(tree.isLoaded("src2"));
    assertTrue(tree.isExpanded("src2"));
    assertTrue(tree.isLoaded(FileTree.ROOT));
  }

  @Test
  public void forgettingANestedFolderKeepsItsParent() {
    FileTree tree = sample().without("src/main");

    assertTrue(tree.isLoaded("src"));
    assertTrue(tree.isExpanded("src"));
    assertFalse(tree.isLoaded("src/main"));
  }

  @Test
  public void forgettingAnUnknownPathChangesNothing() {
    FileTree tree = sample().without("nothing");

    assertEquals(sample().loadedPaths(), tree.loadedPaths());
    assertEquals(sample().rows(), tree.rows());
  }

  @Test
  public void forgettingTheRootEmptiesTheTree() {
    FileTree tree = sample().without(FileTree.ROOT);

    assertTrue(tree.loadedPaths().isEmpty());
    assertTrue(tree.rows().isEmpty());
  }

  @Test
  public void theOriginalTreeIsNotChanged() {
    FileTree before = sample();
    before.without("src");

    assertTrue(before.isLoaded("src/main"));
  }
}
