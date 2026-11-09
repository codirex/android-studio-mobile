package org.codirex.asm.domain.filetree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public final class FileTreeTest {

  private static TreeNode dir(String path) {
    return new TreeNode(path, true);
  }

  private static TreeNode file(String path) {
    return new TreeNode(path, false);
  }

  private static List<String> paths(FileTree tree) {
    List<String> result = new ArrayList<>();
    for (FileTree.Row row : tree.rows()) {
      result.add(row.node().path());
    }
    return result;
  }

  /** Root holds the folder src and the file README.md; src holds Main.java and the folder util. */
  private static FileTree sample() {
    return FileTree.empty()
        .withChildren(FileTree.ROOT, Arrays.asList(dir("src"), file("README.md")))
        .withChildren("src", Arrays.asList(dir("src/util"), file("src/Main.java")));
  }

  @Test
  public void anEmptyTreeHasNoRowsAndNothingLoaded() {
    FileTree tree = FileTree.empty();
    assertTrue(tree.rows().isEmpty());
    assertFalse(tree.isLoaded(FileTree.ROOT));
    assertTrue(tree.loadedPaths().isEmpty());
  }

  @Test
  public void rootEntriesAreShownAtDepthZeroInTheGivenOrder() {
    FileTree tree = sample();
    assertEquals(Arrays.asList("src", "README.md"), paths(tree));
    for (FileTree.Row row : tree.rows()) {
      assertEquals(0, row.depth());
      assertFalse(row.isExpanded());
    }
  }

  @Test
  public void anExpandedFolderIsFollowedByItsIndentedChildren() {
    FileTree tree = sample().withExpanded("src");

    assertEquals(
        Arrays.asList("src", "src/util", "src/Main.java", "README.md"), paths(tree));
    List<FileTree.Row> rows = tree.rows();
    assertTrue(rows.get(0).isExpanded());
    assertEquals(1, rows.get(1).depth());
    assertEquals(1, rows.get(2).depth());
    assertEquals(0, rows.get(3).depth());
  }

  @Test
  public void collapsingHidesChildrenButKeepsThemLoaded() {
    FileTree tree = sample().withExpanded("src").withCollapsed("src");

    assertEquals(Arrays.asList("src", "README.md"), paths(tree));
    assertTrue(tree.isLoaded("src"));
    assertFalse(tree.isExpanded("src"));
  }

  @Test
  public void anExpandedFolderThatIsNotLoadedShowsNoChildrenYet() {
    FileTree tree =
        FileTree.empty()
            .withChildren(FileTree.ROOT, Collections.singletonList(dir("src")))
            .withExpanded("src");

    assertEquals(Collections.singletonList("src"), paths(tree));
    assertTrue(tree.rows().get(0).isExpanded());
    assertFalse(tree.isLoaded("src"));

    FileTree loaded = tree.withChildren("src", Collections.singletonList(file("src/A.java")));
    assertEquals(Arrays.asList("src", "src/A.java"), paths(loaded));
  }

  @Test
  public void toggledOpensAndClosesAFolder() {
    FileTree opened = sample().toggled("src");
    assertTrue(opened.isExpanded("src"));

    FileTree closed = opened.toggled("src");
    assertFalse(closed.isExpanded("src"));
  }

  @Test
  public void nestedFoldersExpandIndependently() {
    FileTree tree =
        sample()
            .withChildren("src/util", Collections.singletonList(file("src/util/Strings.java")))
            .withExpanded("src")
            .withExpanded("src/util");

    assertEquals(
        Arrays.asList(
            "src", "src/util", "src/util/Strings.java", "src/Main.java", "README.md"),
        paths(tree));
    assertEquals(2, tree.rows().get(2).depth());

    FileTree outerClosed = tree.withCollapsed("src");
    assertEquals(Arrays.asList("src", "README.md"), paths(outerClosed));
    // The inner folder stays expanded, so opening the outer one again restores the view.
    assertEquals(paths(tree), paths(outerClosed.withExpanded("src")));
  }

  @Test
  public void filesAreNeverExpanded() {
    FileTree tree = sample().withExpanded("README.md");
    for (FileTree.Row row : tree.rows()) {
      if (row.node().path().equals("README.md")) {
        assertFalse(row.isExpanded());
      }
    }
  }

  @Test
  public void withChildrenReplacesWhatWasThereAndLeavesTheOriginalAlone() {
    FileTree before = sample().withExpanded("src");
    FileTree after = before.withChildren("src", Collections.singletonList(file("src/New.java")));

    assertEquals(Arrays.asList("src", "src/New.java", "README.md"), paths(after));
    assertEquals(
        Arrays.asList("src", "src/util", "src/Main.java", "README.md"), paths(before));
  }

  @Test
  public void loadedPathsListsTheRootAndEveryLoadedFolder() {
    FileTree tree = sample();
    assertTrue(tree.loadedPaths().contains(FileTree.ROOT));
    assertTrue(tree.loadedPaths().contains("src"));
    assertEquals(2, tree.loadedPaths().size());
  }
}
