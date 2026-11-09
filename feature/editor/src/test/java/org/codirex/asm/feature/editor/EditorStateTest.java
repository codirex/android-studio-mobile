package org.codirex.asm.feature.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.codirex.asm.domain.editor.DocumentException.Reason;
import org.codirex.asm.domain.editor.EditorTabs;
import org.junit.Test;

public final class EditorStateTest {

  @Test
  public void startsEmpty() {
    EditorState state = EditorState.initial();
    assertTrue(state.tabs().paths().isEmpty());
    assertNull(state.shownPath());
    assertEquals("", state.shownText());
    assertNull(state.error());
  }

  @Test
  public void showingBumpsTheRevisionEachTime() {
    EditorState state = EditorState.initial().shown("a.txt", "A").shown("b.txt", "B");
    assertEquals(2, state.revision());
    assertEquals("b.txt", state.shownPath());
    assertEquals("B", state.shownText());
  }

  @Test
  public void otherChangesKeepTheRevision() {
    EditorState state = EditorState.initial().shown("a.txt", "A");
    int revision = state.revision();
    state =
        state
            .withTabs(EditorTabs.empty().open("a.txt"))
            .loadingStarted("a.txt")
            .dirtied("a.txt")
            .failed(Reason.STORAGE);
    assertEquals(revision, state.revision());
  }

  @Test
  public void dirtyAndLoadingMarksComeAndGo() {
    EditorState state = EditorState.initial().dirtied("a.txt").loadingStarted("b.txt");
    assertTrue(state.isDirty("a.txt"));
    assertTrue(state.isLoading("b.txt"));

    state = state.cleaned("a.txt").loadingDone("b.txt");
    assertFalse(state.isDirty("a.txt"));
    assertFalse(state.isLoading("b.txt"));
  }

  @Test
  public void removingATabForgetsItsMarks() {
    EditorState state =
        EditorState.initial()
            .withTabs(EditorTabs.empty().open("a.txt").open("b.txt"))
            .dirtied("a.txt")
            .loadingStarted("a.txt")
            .removed("a.txt");

    assertFalse(state.tabs().isOpen("a.txt"));
    assertFalse(state.isDirty("a.txt"));
    assertFalse(state.isLoading("a.txt"));
    assertEquals("b.txt", state.tabs().activePath());
  }

  @Test
  public void anErrorIsShownOnceThenHandled() {
    EditorState state = EditorState.initial().failed(Reason.TOO_LARGE);
    assertEquals(Reason.TOO_LARGE, state.error());
    assertNull(state.errorHandled().error());
  }

  @Test
  public void statesDoNotChangeWhenDerivedFrom() {
    EditorState before = EditorState.initial();
    before.dirtied("a.txt");
    assertFalse(before.isDirty("a.txt"));
  }
}
