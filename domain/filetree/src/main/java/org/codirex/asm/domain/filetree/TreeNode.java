package org.codirex.asm.domain.filetree;

import java.util.Objects;

/**
 * One file or folder of a project, identified by its path relative to the project root with {@code
 * /} as separator, for example {@code app/src/Main.java}. The project root itself is not a node.
 * Immutable.
 */
public final class TreeNode {

  private final String path;
  private final String name;
  private final boolean directory;

  /**
   * @param path relative path, not empty, without a leading or trailing slash
   * @param directory whether the node is a folder
   * @throws IllegalArgumentException if {@code path} is malformed
   */
  public TreeNode(String path, boolean directory) {
    Objects.requireNonNull(path, "path");
    if (path.isEmpty() || path.startsWith("/") || path.endsWith("/")) {
      throw new IllegalArgumentException("invalid path: '" + path + "'");
    }
    this.path = path;
    this.directory = directory;
    int slash = path.lastIndexOf('/');
    this.name = slash < 0 ? path : path.substring(slash + 1);
  }

  /** The relative path, unique inside a project. */
  public String path() {
    return path;
  }

  /** The last segment of the path. */
  public String name() {
    return name;
  }

  public boolean isDirectory() {
    return directory;
  }

  /** The path of the folder that contains this node; empty for nodes directly under the root. */
  public String parentPath() {
    int slash = path.lastIndexOf('/');
    return slash < 0 ? "" : path.substring(0, slash);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof TreeNode)) {
      return false;
    }
    TreeNode that = (TreeNode) other;
    return directory == that.directory && path.equals(that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, directory);
  }

  @Override
  public String toString() {
    return (directory ? "dir " : "file ") + path;
  }
}
