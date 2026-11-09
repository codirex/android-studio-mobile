package org.codirex.asm.feature.filetree;

/**
 * Something the host must be told once: a created file to open, a deleted path or a renamed path.
 * Immutable.
 */
final class TreeEvent {

  enum Type {
    /** A new file was created and should be opened. */
    OPEN,
    /** A file or folder was deleted. */
    DELETED,
    /** A file or folder was renamed. */
    RENAMED
  }

  private final Type type;
  private final String path;
  private final String newPath;

  private TreeEvent(Type type, String path, String newPath) {
    this.type = type;
    this.path = path;
    this.newPath = newPath;
  }

  static TreeEvent open(String path) {
    return new TreeEvent(Type.OPEN, path, null);
  }

  static TreeEvent deleted(String path) {
    return new TreeEvent(Type.DELETED, path, null);
  }

  static TreeEvent renamed(String oldPath, String newPath) {
    return new TreeEvent(Type.RENAMED, oldPath, newPath);
  }

  Type type() {
    return type;
  }

  /** The path concerned; for a rename, the old path. */
  String path() {
    return path;
  }

  /** The new path of a rename; null for the other events. */
  String newPath() {
    return newPath;
  }
}
