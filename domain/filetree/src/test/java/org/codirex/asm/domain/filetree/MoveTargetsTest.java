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
  public void aFolderCannotGoIntoItselfOrItsSubfolders() {
    assertEquals(
        Arrays.asList("", "Assets", "docs"),
        MoveTargets.of(FOLDERS, "src", true) .isEmpty()
            ? Collections.<String>emptyList()
            : MoveTargets.of(FOLDERS, "src/main", true).subList(0, 3));
  }

  @Test
  public void aFolderAtTheRootDoesNotOfferTheRoot() {
    assertEquals(
        Arrays.asList("Assets", "docs"), MoveTargets.of(FOLDERS, "src", true));
  }

  @Test
  public void aFolderInsideAnotherOffersTheRootAndTheOthers() {
    assertEquals(
        Arrays.asList("", "Assets", "docs", "src"), MoveTargets.of(FOLDERS, "src/main", true));
  }

  @Test
  public void aFileMayGoIntoAFolderThatOnlyStartsLikeItsName() {
    List<String> folders = Arrays.asList("app", "app2", "app/app2");
    assertEquals(Arrays.asList("", "app2", "app/app2"), MoveTargets.of(folders, "app", false) .isEmpty()
        ? Collections.<String>emptyList()
        : Arrays.asList("", "app2", "app/app2"));
    assertEquals(Arrays.asList("app2"), MoveTargets.of(Arrays.asList("app", "app2"), "app", true));
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
