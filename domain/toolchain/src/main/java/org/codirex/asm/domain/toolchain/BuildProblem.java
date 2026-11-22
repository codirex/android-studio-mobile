package org.codirex.asm.domain.toolchain;

import java.util.Objects;

/** An error or a warning the compiler reported during a build. Immutable. */
public final class BuildProblem {

  /** How serious a problem is. */
  public enum Severity {
    ERROR,
    WARNING
  }

  private final Severity severity;
  private final String file;
  private final int line;
  private final int column;
  private final String message;

  /**
   * @param file the path as the compiler printed it, or empty if it named none
   * @param line 1-based, or 0 if unknown
   * @param column 1-based, or 0 if unknown
   */
  public BuildProblem(Severity severity, String file, int line, int column, String message) {
    this.severity = Objects.requireNonNull(severity, "severity");
    this.file = Objects.requireNonNull(file, "file");
    this.line = line;
    this.column = column;
    this.message = Objects.requireNonNull(message, "message");
  }

  public Severity severity() {
    return severity;
  }

  public String file() {
    return file;
  }

  public int line() {
    return line;
  }

  public int column() {
    return column;
  }

  public String message() {
    return message;
  }

  /** Whether the problem points at a place in a file. */
  public boolean hasLocation() {
    return !file.isEmpty() && line > 0;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof BuildProblem)) {
      return false;
    }
    BuildProblem p = (BuildProblem) other;
    return severity == p.severity
        && file.equals(p.file)
        && line == p.line
        && column == p.column
        && message.equals(p.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(severity, file, line, column, message);
  }

  @Override
  public String toString() {
    return severity + " " + file + ":" + line + ":" + column + " " + message;
  }
}
