package org.codirex.asm.domain.lsp;

import java.util.Collections;
import java.util.List;
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
  private final List<TextEdit> additionalEdits;
  private final int caretOffset;

  public CompletionItem(
      String label,
      String detail,
      int kind,
      String insertText,
      int startLine,
      int startCharacter,
      int endLine,
      int endCharacter) {
    this(label, detail, kind, insertText, startLine, startCharacter, endLine, endCharacter, null, -1);
  }

  /**
   * @param additionalEdits changes elsewhere in the document that come with the proposal (an
   *     import), in the places of the document before the proposal is taken; may be null
   * @param caretOffset where the caret goes inside {@code insertText}, or -1 for its end
   */
  public CompletionItem(
      String label,
      String detail,
      int kind,
      String insertText,
      int startLine,
      int startCharacter,
      int endLine,
      int endCharacter,
      List<TextEdit> additionalEdits,
      int caretOffset) {
    this.label = Objects.requireNonNull(label, "label");
    this.detail = Objects.requireNonNull(detail, "detail");
    this.kind = kind;
    this.insertText = Objects.requireNonNull(insertText, "insertText");
    this.startLine = startLine;
    this.startCharacter = startCharacter;
    this.endLine = endLine;
    this.endCharacter = endCharacter;
    this.additionalEdits =
        additionalEdits == null ? Collections.<TextEdit>emptyList() : Collections.unmodifiableList(additionalEdits);
    this.caretOffset = caretOffset;
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

  /** Changes elsewhere that belong to the proposal, last position first; empty if none. */
  public List<TextEdit> additionalEdits() {
    return additionalEdits;
  }

  /** Where the caret goes inside {@link #insertText()}; -1 for its end. */
  public int caretOffset() {
    return caretOffset;
  }

  @Override
  public String toString() {
    return "CompletionItem(" + label + ")";
  }
}
