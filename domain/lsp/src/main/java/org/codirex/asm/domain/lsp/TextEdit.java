package org.codirex.asm.domain.lsp;

import java.util.Objects;

/** A change the server asks for: the text of a range (zero based, end exclusive) becomes {@link #newText()}. Immutable. */
public final class TextEdit {

  private final int startLine;
  private final int startCharacter;
  private final int endLine;
  private final int endCharacter;
  private final String newText;

  public TextEdit(
      int startLine, int startCharacter, int endLine, int endCharacter, String newText) {
    this.startLine = startLine;
    this.startCharacter = startCharacter;
    this.endLine = endLine;
    this.endCharacter = endCharacter;
    this.newText = Objects.requireNonNull(newText, "newText");
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

  public String newText() {
    return newText;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof TextEdit)) {
      return false;
    }
    TextEdit e = (TextEdit) other;
    return startLine == e.startLine
        && startCharacter == e.startCharacter
        && endLine == e.endLine
        && endCharacter == e.endCharacter
        && newText.equals(e.newText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startLine, startCharacter, endLine, endCharacter, newText);
  }
}
