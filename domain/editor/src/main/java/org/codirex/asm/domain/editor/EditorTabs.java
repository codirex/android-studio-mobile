package org.codirex.asm.domain.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The open files of a project and the active one. A file is identified by its path relative to the
 * project root. Immutable: every change returns a new value.
 *
 * <p>Tabs keep the order in which files were opened. The active path is {@code null} when no file
 * is open.
 */
public final class EditorTabs {

  private static final EditorTabs EMPTY = new EditorTabs(Collections.<String>emptyList(), null);

  private final List<String> paths;
  private final String active;

  private EditorTabs(List<String> paths, String active) {
    this.paths = Collections.unmodifiableList(new ArrayList<>(paths));
    this.active = active;
  }

  /** No file is open. */
  public static EditorTabs empty() {
    return EMPTY;
  }

  /** The open files, in the order they were opened. */
  public List<String> paths() {
    return paths;
  }

  /** The active file, or {@code null} if none is open. */
  public String activePath() {
    return active;
  }

  public boolean isOpen(String path) {
    return paths.contains(path);
  }

  /**
   * Opens a file and makes it active. A file that is already open is only made active, it keeps its
   * place.
   */
  public EditorTabs open(String path) {
    Objects.requireNonNull(path, "path");
    if (paths.contains(path)) {
      return select(path);
    }
    List<String> copy = new ArrayList<>(paths);
    copy.add(path);
    return new EditorTabs(copy, path);
  }

  /** Makes an open file active. Does nothing if the file is not open. */
  public EditorTabs select(String path) {
    if (!paths.contains(path) || path.equals(active)) {
      return this;
    }
    return new EditorTabs(paths, path);
  }

  /**
   * Closes a file. If it was the active one, its right neighbour becomes active, or the left one
   * when it was the last tab. Does nothing if the file is not open.
   */
  public EditorTabs close(String path) {
    int index = paths.indexOf(path);
    if (index < 0) {
      return this;
    }
    List<String> copy = new ArrayList<>(paths);
    copy.remove(index);
    String newActive = active;
    if (path.equals(active)) {
      newActive = copy.isEmpty() ? null : copy.get(Math.min(index, copy.size() - 1));
    }
    return new EditorTabs(copy, newActive);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof EditorTabs)) {
      return false;
    }
    EditorTabs that = (EditorTabs) other;
    return paths.equals(that.paths) && Objects.equals(active, that.active);
  }

  @Override
  public int hashCode() {
    return Objects.hash(paths, active);
  }

  @Override
  public String toString() {
    return "EditorTabs(" + paths + ", active=" + active + ")";
  }
}
