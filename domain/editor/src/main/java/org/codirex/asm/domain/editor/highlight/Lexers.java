package org.codirex.asm.domain.editor.highlight;

import org.codirex.asm.domain.editor.SourceLanguage;

/** Finds the lexer of a language. */
public final class Lexers {
  private Lexers() {}

  /** The lexer for {@code language}, or null when the editor has another way to highlight it. */
  public static Lexer forLanguage(SourceLanguage language) {
    switch (language) {
      case XML:
        return new XmlLexer();
      case JSON:
        return new JsonLexer();
      case MARKDOWN:
        return new MarkdownLexer();
      case GRADLE:
        return new GradleLexer();
      case JAVA:
      case PLAIN:
      default:
        return null;
    }
  }
}
