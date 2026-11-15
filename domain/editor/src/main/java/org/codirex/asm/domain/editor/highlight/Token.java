package org.codirex.asm.domain.editor.highlight;

import java.util.Objects;

/** A piece of text, {@code [start, end)}, that has a {@link TokenKind}. */
public final class Token {
  private final int start;
  private final int end;
  private final TokenKind kind;

  public Token(int start, int end, TokenKind kind) {
    if (start < 0 || end <= start) {
      throw new IllegalArgumentException("Bad token range " + start + ".." + end);
    }
    this.start = start;
    this.end = end;
    this.kind = Objects.requireNonNull(kind);
  }

  public int start() {
    return start;
  }

  /** The index after the last character. */
  public int end() {
    return end;
  }

  public TokenKind kind() {
    return kind;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Token)) {
      return false;
    }
    Token token = (Token) other;
    return start == token.start && end == token.end && kind == token.kind;
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end, kind);
  }

  @Override
  public String toString() {
    return kind + "[" + start + "," + end + ")";
  }
}
