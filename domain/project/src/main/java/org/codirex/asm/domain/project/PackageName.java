package org.codirex.asm.domain.project;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.codirex.asm.domain.common.Validation;

/**
 * A package name that is valid as an Android application id and as a Java package: at least two
 * segments separated by dots, each starting with a letter and made of letters, digits and
 * underscores, none of them a Java keyword.
 */
public final class PackageName {

  public static final String ERROR_EMPTY = "package.empty";
  public static final String ERROR_TOO_FEW_SEGMENTS = "package.too_few_segments";
  public static final String ERROR_SEGMENT_START = "package.segment_start";
  public static final String ERROR_CHARACTER = "package.character";
  public static final String ERROR_KEYWORD = "package.keyword";

  private static final Set<String> KEYWORDS =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
                  "class", "const", "continue", "default", "do", "double", "else", "enum",
                  "extends", "final", "finally", "float", "for", "goto", "if", "implements",
                  "import", "instanceof", "int", "interface", "long", "native", "new", "package",
                  "private", "protected", "public", "return", "short", "static", "strictfp",
                  "super", "switch", "synchronized", "this", "throw", "throws", "transient",
                  "try", "void", "volatile", "while", "true", "false", "null")));

  private final String value;

  private PackageName(String value) {
    this.value = value;
  }

  /** Checks {@code candidate} without creating anything. Use it to validate input as the user types. */
  public static Validation validate(String candidate) {
    if (candidate == null || candidate.isEmpty()) {
      return Validation.error(ERROR_EMPTY);
    }
    String[] segments = candidate.split("\\.", -1);
    if (segments.length < 2) {
      return Validation.error(ERROR_TOO_FEW_SEGMENTS);
    }
    for (String segment : segments) {
      if (segment.isEmpty() || !isAsciiLetter(segment.charAt(0))) {
        return Validation.error(ERROR_SEGMENT_START);
      }
      for (int i = 1; i < segment.length(); i++) {
        char c = segment.charAt(i);
        if (!isAsciiLetter(c) && !(c >= '0' && c <= '9') && c != '_') {
          return Validation.error(ERROR_CHARACTER);
        }
      }
      if (KEYWORDS.contains(segment)) {
        return Validation.error(ERROR_KEYWORD);
      }
    }
    return Validation.ok();
  }

  /**
   * Creates a package name.
   *
   * @throws IllegalArgumentException with the error code as message if {@code candidate} is invalid
   */
  public static PackageName of(String candidate) {
    Validation validation = validate(candidate);
    if (!validation.isValid()) {
      throw new IllegalArgumentException(validation.error());
    }
    return new PackageName(candidate);
  }

  /** The dotted name, for example {@code org.codirex.demo}. */
  public String value() {
    return value;
  }

  /** The source directory for this package, with {@code /} separators: {@code org/codirex/demo}. */
  public String toDirectoryPath() {
    return value.replace('.', '/');
  }

  private static boolean isAsciiLetter(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof PackageName && value.equals(((PackageName) other).value);
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
