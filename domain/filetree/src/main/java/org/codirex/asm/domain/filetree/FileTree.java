package org.codirex.asm.domain.filetree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The state of a file tree: which folders have been loaded and which are expanded. It is an
 * immutable value; every change returns a new tree. It does no I/O: folders are loaded elsewhere and
 * handed in with {@link #withChildren}.
 *
 * <p>The root folder (path {@link #ROOT}) is always shown. A folder that is expanded but not loaded
 * yet shows no children until they arrive.
 */
public final class FileTree {

  /** The path of the project root. */
  public static final String ROOT = "";

  /** One visible line of the tree. Immutable. */
  public static final class Row {
    private final TreeNode node;
    private final int depth;
    private final boolean expanded;

    Row(TreeNode node, int depth, boolean expanded) {
      this.node = node;
      this.depth = depth;
      this.expanded = expanded;
    }

    public TreeNode node() {
      return node;
    }

    /** 0 for entries directly under the root. */
    public int depth() {
      return depth;
    }

    /** True for a folder that is open. Always false for files. */
    public boolean isExpanded() {
      return expanded;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Row)) {
        return false;
      }
      Row that = (Row) other;
      return depth == that.depth && expanded == that.expanded && node.equals(that.node);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, depth, expanded);
    }

    @Override
    public String toString() {
      return "Row(" + node + ", depth=" + depth + ", expanded=" + expanded + ")";
    }
  }

  private static final FileTree EMPTY =
      new FileTree(Collections.<String, List<TreeNode>>emptyMap(), Collections.<String>emptySet());

  private final Map<String, List<TreeNode>> children;
  private final Set<String> expanded;

  private FileTree(Map<String, List<TreeNode>> children, Set<String> expanded) {
    this.children = Collections.unmodifiableMap(new LinkedHashMap<>(children));
    this.expanded = Collections.unmodifiableSet(new LinkedHashSet<>(expanded));
  }

  /** A tree with nothing loaded. */
  public static FileTree empty() {
    return EMPTY;
  }

  /** Whether the entries of this folder are known. */
  public boolean isLoaded(String path) {
    return children.containsKey(path);
  }

  public boolean isExpanded(String path) {
    return expanded.contains(path);
  }

  /** The paths of all loaded folders, the root included once it is loaded. */
  public Set<String> loadedPaths() {
    return children.keySet();
  }

  /** Returns a tree where {@code path} holds {@code nodes}, replacing what it held before. */
  public FileTree withChildren(String path, List<TreeNode> nodes) {
    Map<String, List<TreeNode>> copy = new LinkedHashMap<>(children);
    copy.put(path, Collections.unmodifiableList(new ArrayList<>(nodes)));
    return new FileTree(copy, expanded);
  }

  /** Returns a tree with the folder opened. Its children stay unloaded until they are handed in. */
  public FileTree withExpanded(String path) {
    if (expanded.contains(path)) {
      return this;
    }
    Set<String> copy = new LinkedHashSet<>(expanded);
    copy.add(path);
    return new FileTree(children, copy);
  }

  /** Returns a tree with the folder closed. Its loaded children are kept. */
  public FileTree withCollapsed(String path) {
    if (!expanded.contains(path)) {
      return this;
    }
    Set<String> copy = new LinkedHashSet<>(expanded);
    copy.remove(path);
    return new FileTree(children, copy);
  }

  /**
   * Returns a tree that forgets a folder and everything under it: what was loaded and what was
   * open. Use it when the folder was deleted or renamed, so a refresh does not list it again. The
   * entry in its parent's list is replaced when the parent is loaded again. Forgetting the root
   * gives an empty tree.
   */
  public FileTree without(String path) {
    if (path.equals(ROOT)) {
      return EMPTY;
    }
    String prefix = path + "/";
    Map<String, List<TreeNode>> loaded = new LinkedHashMap<>(children);
    Set<String> open = new LinkedHashSet<>(expanded);
    loaded.keySet().removeIf(key -> key.equals(path) || key.startsWith(prefix));
    open.removeIf(key -> key.equals(path) || key.startsWith(prefix));
    return new FileTree(loaded, open);
  }

  /** Opens a closed folder and closes an open one. */
  public FileTree toggled(String path) {
    return isExpanded(path) ? withCollapsed(path) : withExpanded(path);
  }

  /** The visible lines, top to bottom: a folder is followed by its children when it is open. */
  public List<Row> rows() {
    List<Row> rows = new ArrayList<>();
    appendChildren(ROOT, 0, rows);
    return rows;
  }

  private void appendChildren(String parent, int depth, List<Row> rows) {
    List<TreeNode> nodes = children.get(parent);
    if (nodes == null) {
      return;
    }
    for (TreeNode node : nodes) {
      boolean open = node.isDirectory() && expanded.contains(node.path());
      rows.add(new Row(node, depth, open));
      if (open) {
        appendChildren(node.path(), depth + 1, rows);
      }
    }
  }
}
