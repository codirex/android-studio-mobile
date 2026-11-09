package org.codirex.asm.domain.filetree;

/** Which names a file or folder of a project may have. A name is one path segment. */
public final class EntryNames {

  /** The longest name accepted, in characters. */
  public static final int MAX_LENGTH = 255;

  private EntryNames() {}

  /**
   * Whether {@code name} can be created: not empty, not longer than {@link #MAX_LENGTH}, not {@code
   * .} or {@code ..}, and without {@code /} or NUL characters. Surrounding spaces are the caller's
   * business: trim before asking.
   */
  public static boolean isValid(String name) {
    if (name == null || name.isEmpty() || name.length() > MAX_LENGTH) {
      return false;
    }
    if (name.equals(".") || name.equals("..")) {
      return false;
    }
    return name.indexOf('/') < 0 && name.indexOf('\0') < 0;
  }
}
