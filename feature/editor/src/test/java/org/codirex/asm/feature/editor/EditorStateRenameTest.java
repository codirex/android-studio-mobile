package org.codirex.asm.feature.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.codirex.asm.domain.editor.EditorTabs;
import org.junit.Test;

public final class EditorStateRenameTest {

  private static EditorState sample() {
    return EditorState.initial()
        .withTabs(EditorTabs.empty().open("src/a.txt").open("src/b.txt").open("other.txt"))
        .dirtied("src/a.txt")
        .dirtied("other.txt")
        .loadingStarted("src/b.txt")
        .shown("src/a.txt", "A");
  }

  @Test
  public void renamingAFolderMovesTabsMarksAndTheShownPath() {
    EditorState state = sample().renamed("src", "lib");

    assertEquals(Arrays.asList("lib/a.txt", "lib/b.txt", "other.txt"), state.tabs().paths());
    assertTrue(state.isDirty("lib/a.txt"));
    assertTrue(state.isDirty("other.txt"));
    assertFalse(state.isDirty("src/a.txt"));
    assertTrue(state.isLoading("lib/b.txt"));
    assertEquals("lib/a.txt", state.shownPath());
  }

  @Test
  public void renamingKeepsTheTextAndTheRevisionSoTheWidgetIsNotReset() {
    EditorState before = sample();
    EditorState state = before.renamed("src/a.txt", "src/c.txt");

    assertEquals(before.revision(), state.revision());
    assertEquals("A", state.shownText());
    assertEquals("src/c.txt", state.shownPath());
  }

  @Test
  public void renamingSomethingNotOpenChangesNothing() {
    EditorState before = sample();
    EditorState state = before.renamed("nothing", "x");

    assertEquals(before.tabs(), state.tabs());
    assertEquals(before.dirty(), state.dirty());
    assertEquals(before.shownPath(), state.shownPath());
  }

  @Test
  public void renamingWithNothingShownKeepsItNull() {
    EditorState state = EditorState.initial().renamed("a", "b");

    assertNull(state.shownPath());
  }
}
