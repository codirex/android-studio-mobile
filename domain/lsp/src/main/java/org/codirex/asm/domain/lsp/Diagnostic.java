package org.codirex.asm.domain.lsp;

import java.util.Objects;

/** A problem the server found in a document. Lines and characters count from zero. Immutable. */
public final class Diagnostic {

  /** The severities of the protocol. */
  public static final int ERROR = 1;

  public static final int WARNING = 2;
  public static final int INFORMATION = 3;
  public static final int HINT = 4;

  private final int startLine;
  private final int startCharacter;
  private final int endLine;
  private final int endCharacter;
  private final int severity;
  private final String message;

  public Diagnostic(
      int startLine,
      int startCharacter,
      int endLine,
      int endCharacter,
      int severity,
      String message) {
    this.startLine = startLine;
    this.startCharacter = startCharacter;
    this.endLine = endLine;
    this.endCharacter = endCharacter;
    this.severity = severity;
    this.message = Objects.requireNonNull(message, "message");
  }

  public int startLine() {
    return startLine;
  }

  public int startCharacter() {
    return startCharacter;
  }

  public int endLine() {
    return endLine;
  }

  public int endCharacter() {
    return endCharacter;
  }

  /** One of the constants above; {@link #ERROR} when the server gave none. */
  public int severity() {
    return severity;
  }

  public String message() {
    return message;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Diagnostic)) {
      return false;
    }
    Diagnostic that = (Diagnostic) other;
    return startLine == that.startLine
        && startCharacter == that.startCharacter
        && endLine == that.endLine
        && endCharacter == that.endCharacter
        && severity == that.severity
        && message.equals(that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startLine, startCharacter, endLine, endCharacter, severity, message);
  }

  @Override
  public String toString() {
    return "Diagnostic(" + startLine + ":" + startCharacter + " " + message + ")";
  }
}
