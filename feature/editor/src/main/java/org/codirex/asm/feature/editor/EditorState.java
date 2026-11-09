package org.codirex.asm.feature.editor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.codirex.asm.domain.editor.DocumentException.Reason;
import org.codirex.asm.domain.editor.EditorTabs;

/**
 * Everything the editor screen shows. Immutable. The text of the documents is not here: it is large
 * and changes with every key press, so the view model keeps it.
 *
 * <p>{@link #revision()} grows each time the editor widget must be given new text, which is when
 * another document becomes the shown one or a document finishes loading. {@link #error()} is a
 * one-time message that the screen shows and then reports as handled.
 */
final class EditorState {

  private final EditorTabs tabs;
  private final Set<String> dirty;
  private final Set<String> loading;
  private final String shownPath;
  private final String shownText;
  private final int revision;
  private final Reason error;

  private EditorState(
      EditorTabs tabs,
      Set<String> dirty,
      Set<String> loading,
      String shownPath,
      String shownText,
      int revision,
      Reason error) {
    this.tabs = tabs;
    this.dirty = Collections.unmodifiableSet(new HashSet<>(dirty));
    this.loading = Collections.unmodifiableSet(new HashSet<>(loading));
    this.shownPath = shownPath;
    this.shownText = shownText;
    this.revision = revision;
    this.error = error;
  }

  static EditorState initial() {
    return new EditorState(
        EditorTabs.empty(),
        Collections.<String>emptySet(),
        Collections.<String>emptySet(),
        null,
        "",
        0,
        null);
  }

  EditorState withTabs(EditorTabs newTabs) {
    return new EditorState(newTabs, dirty, loading, shownPath, shownText, revision, error);
  }

  EditorState loadingStarted(String path) {
    Set<String> copy = new HashSet<>(loading);
    copy.add(path);
    return new EditorState(tabs, dirty, copy, shownPath, shownText, revision, error);
  }

  EditorState loadingDone(String path) {
    Set<String> copy = new HashSet<>(loading);
    copy.remove(path);
    return new EditorState(tabs, dirty, copy, shownPath, shownText, revision, error);
  }

  EditorState dirtied(String path) {
    Set<String> copy = new HashSet<>(dirty);
    copy.add(path);
    return new EditorState(tabs, copy, loading, shownPath, shownText, revision, error);
  }

  EditorState cleaned(String path) {
    Set<String> copy = new HashSet<>(dirty);
    copy.remove(path);
    return new EditorState(tabs, copy, loading, shownPath, shownText, revision, error);
  }

  /** The editor must now show {@code text} of {@code path}; {@code path} is null for nothing. */
  EditorState shown(String path, String text) {
    return new EditorState(tabs, dirty, loading, path, text, revision + 1, error);
  }

  /** Closes a tab and forgets its dirty and loading marks. The shown document is not changed. */
  EditorState removed(String path) {
    Set<String> newDirty = new HashSet<>(dirty);
    newDirty.remove(path);
    Set<String> newLoading = new HashSet<>(loading);
    newLoading.remove(path);
    return new EditorState(
        tabs.close(path), newDirty, newLoading, shownPath, shownText, revision, error);
  }

  /**
   * {@code from} was renamed to {@code to}: its tab, the tabs of the files inside it and their
   * marks move along. The shown text stays and so does the revision, because the editor widget
   * already holds that text.
   */
  EditorState renamed(String from, String to) {
    return new EditorState(
        tabs.renamed(from, to),
        remapAll(dirty, from, to),
        remapAll(loading, from, to),
        shownPath == null ? null : EditorTabs.remapPath(shownPath, from, to),
        shownText,
        revision,
        error);
  }

  private static Set<String> remapAll(Set<String> paths, String from, String to) {
    Set<String> result = new HashSet<>();
    for (String path : paths) {
      result.add(EditorTabs.remapPath(path, from, to));
    }
    return result;
  }

  EditorState failed(Reason reason) {
    return new EditorState(tabs, dirty, loading, shownPath, shownText, revision, reason);
  }

  EditorState errorHandled() {
    return new EditorState(tabs, dirty, loading, shownPath, shownText, revision, null);
  }

  EditorTabs tabs() {
    return tabs;
  }

  /** The paths with unsaved changes. */
  Set<String> dirty() {
    return dirty;
  }

  boolean isDirty(String path) {
    return dirty.contains(path);
  }

  /** Whether the file is still being read. */
  boolean isLoading(String path) {
    return loading.contains(path);
  }

  /** The document the editor widget shows, or null if it shows nothing. */
  String shownPath() {
    return shownPath;
  }

  /** The text to put into the widget when {@link #revision()} changed. */
  String shownText() {
    return shownText;
  }

  int revision() {
    return revision;
  }

  /** A failure not yet shown to the user, or null. */
  Reason error() {
    return error;
  }
}
