package org.codirex.asm.domain.filetree;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public final class MoveTargetsTest {

  private static final List<String> FOLDERS =
      Arrays.asList("src", "src/main", "src/main/res", "docs", "Assets");

  @Test
  public void aFileAtTheRootCanGoToEveryFolderSortedIgnoringCase() {
    assertEquals(
        Arrays.asList("Assets", "docs", "src", "src/main", "src/main/res"),
        MoveTargets.of(FOLDERS, "a.txt", false));
  }

  @Test
  public void aFileInAFolderCanGoToTheRootButNotBackToItsOwnFolder() {
    assertEquals(
        Arrays.asList("", "Assets", "docs", "src", "src/main/res"),
        MoveTargets.of(FOLDERS, "src/main/A.java", false));
  }

  @Test
  public void aFolderAtTheRootCannotGoIntoItselfOrItsSubfolders() {
    // "src" is at the root, so the root is not offered; src, src/main and src/main/res are inside it.
    assertEquals(Arrays.asList("Assets", "docs"), MoveTargets.of(FOLDERS, "src", true));
  }

  @Test
  public void aFolderInsideAnotherOffersTheRootAndTheOthers() {
    // src is its parent, src/main is itself, src/main/res is inside it.
    assertEquals(
        Arrays.asList("", "Assets", "docs"), MoveTargets.of(FOLDERS, "src/main", true));
  }

  @Test
  public void aFolderMayGoIntoOneThatOnlyStartsLikeItsName() {
    // app2 starts with "app" but is not inside it.
    assertEquals(
        Arrays.asList("app2"), MoveTargets.of(Arrays.asList("app", "app2"), "app", true));
  }

  @Test
  public void aFileNamedLikeAFolderIsNotTreatedAsThatFolder() {
    // A file is never a target, so only folders of the same name prefix are filtered for folders.
    assertEquals(
        Arrays.asList("app", "app/app2", "app2"),
        MoveTargets.of(Arrays.asList("app", "app2", "app/app2"), "app", false));
  }

  @Test
  public void noFoldersAtAllOffersOnlyTheRootForAnEntryInAFolder() {
    assertEquals(
        Collections.singletonList(""),
        MoveTargets.of(Collections.<String>emptyList(), "x/a.txt", false));
    assertEquals(
        Collections.<String>emptyList(),
        MoveTargets.of(Collections.<String>emptyList(), "a.txt", false));
  }
}
