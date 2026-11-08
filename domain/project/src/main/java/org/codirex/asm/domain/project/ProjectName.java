package org.codirex.asm.domain.project;

import org.codirex.asm.domain.common.Validation;

/**
 * A project name that is safe to use as a directory name on Android: not empty, at most 64
 * characters, no path separators or characters that file systems reserve, no control characters, no
 * leading or trailing space, no trailing dot, and not {@code .} or {@code ..}.
 */
public final class ProjectName {

  public static final int MAX_LENGTH = 64;

  public static final String ERROR_EMPTY = "name.empty";
  public static final String ERROR_TOO_LONG = "name.too_long";
  public static final String ERROR_RESERVED = "name.reserved";
  public static final String ERROR_CHARACTER = "name.character";

  private static final String FORBIDDEN = "/\\:*?\"<>|";

  private final String value;

  private ProjectName(String value) {
    this.value = value;
  }

  /** Checks {@code candidate} without creating anything. Use it to validate input as the user types. */
  public static Validation validate(String candidate) {
    if (candidate == null || candidate.trim().isEmpty()) {
      return Validation.error(ERROR_EMPTY);
    }
    if (candidate.length() > MAX_LENGTH) {
      return Validation.error(ERROR_TOO_LONG);
    }
    if (candidate.equals(".") || candidate.equals("..")) {
      return Validation.error(ERROR_RESERVED);
    }
    if (candidate.charAt(0) == ' '
        || candidate.charAt(candidate.length() - 1) == ' '
        || candidate.charAt(candidate.length() - 1) == '.') {
      return Validation.error(ERROR_CHARACTER);
    }
    for (int i = 0; i < candidate.length(); i++) {
      char c = candidate.charAt(i);
      if (c < 0x20 || c == 0x7F || FORBIDDEN.indexOf(c) >= 0) {
        return Validation.error(ERROR_CHARACTER);
      }
    }
    return Validation.ok();
  }

  /**
   * Creates a project name.
   *
   * @throws IllegalArgumentException with the error code as message if {@code candidate} is invalid
   */
  public static ProjectName of(String candidate) {
    Validation validation = validate(candidate);
    if (!validation.isValid()) {
      throw new IllegalArgumentException(validation.error());
    }
    return new ProjectName(candidate);
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ProjectName && value.equals(((ProjectName) other).value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value;
  }
}
