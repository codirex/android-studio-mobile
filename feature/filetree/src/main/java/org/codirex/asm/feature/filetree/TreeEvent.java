package org.codirex.asm.feature.filetree;

/** Something the host must be told once: a created file to open, or a deleted path. Immutable. */
final class TreeEvent {

  enum Type {
    /** A new file was created and should be opened. */
    OPEN,
    /** A file or folder was deleted. */
    DELETED
  }

  private final Type type;
  private final String path;

  private TreeEvent(Type type, String path) {
    this.type = type;
    this.path = path;
  }

  static TreeEvent open(String path) {
    return new TreeEvent(Type.OPEN, path);
  }

  static TreeEvent deleted(String path) {
    return new TreeEvent(Type.DELETED, path);
  }

  Type type() {
    return type;
  }

  String path() {
    return path;
  }
}
