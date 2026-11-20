package org.codirex.asm.domain.lsp;

import java.util.Objects;

/**
 * One proposal of the server. {@link #insertText()} is what to put in; when the server gave a range
 * ({@link #hasRange()}), the text in that range is replaced, otherwise the word before the caret.
 * Immutable.
 */
public final class CompletionItem {

  private final String label;
  private final String detail;
  private final int kind;
  private final String insertText;
  private final int startLine;
  private final int startCharacter;
  private final int endLine;
  private final int endCharacter;

  public CompletionItem(
      String label,
      String detail,
      int kind,
      String insertText,
      int startLine,
      int startCharacter,
      int endLine,
      int endCharacter) {
    this.label = Objects.requireNonNull(label, "label");
    this.detail = Objects.requireNonNull(detail, "detail");
    this.kind = kind;
    this.insertText = Objects.requireNonNull(insertText, "insertText");
    this.startLine = startLine;
    this.startCharacter = startCharacter;
    this.endLine = endLine;
    this.endCharacter = endCharacter;
  }

  public String label() {
    return label;
  }

  /** The type or signature the server shows next to the label; empty if none. */
  public String detail() {
    return detail;
  }

  /** The kind of the protocol (1 text, 2 method, 3 function, 5 field, 7 class, ...); 0 if none. */
  public int kind() {
    return kind;
  }

  public String insertText() {
    return insertText;
  }

  public boolean hasRange() {
    return startLine >= 0;
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

  @Override
  public String toString() {
    return "CompletionItem(" + label + ")";
  }
}
