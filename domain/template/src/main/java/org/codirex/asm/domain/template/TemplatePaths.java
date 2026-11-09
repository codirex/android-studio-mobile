package org.codirex.asm.domain.template;

/** Which paths a template may write to. */
public final class TemplatePaths {

  private static final String INTERNAL_DIRECTORY = ".asm";

  private TemplatePaths() {}

  /**
   * Whether {@code path} is a safe place for a template file: relative, with {@code /} as the only
   * separator, without empty, {@code .} or {@code ..} segments, without NUL characters, and not
   * inside the project's internal folder.
   */
  public static boolean isSafe(String path) {
    if (path == null
        || path.isEmpty()
        || path.startsWith("/")
        || path.indexOf('\\') >= 0
        || path.indexOf('\0') >= 0) {
      return false;
    }
    String[] segments = path.split("/", -1);
    for (String segment : segments) {
      if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
        return false;
      }
    }
    return !segments[0].equals(INTERNAL_DIRECTORY);
  }
}
