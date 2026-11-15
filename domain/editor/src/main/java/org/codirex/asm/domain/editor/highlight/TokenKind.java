package org.codirex.asm.domain.editor.highlight;

/** What a highlighted piece of text is. The editor decides the colour of each kind. */
public enum TokenKind {
  KEYWORD,
  COMMENT,
  STRING,
  NUMBER,
  /** An XML tag, with its angle brackets and the name. */
  TAG,
  ATTRIBUTE_NAME,
  ATTRIBUTE_VALUE,
  OPERATOR,
  /** A name the language gives a special meaning, such as a Gradle block. */
  BUILTIN,
  HEADING,
  CODE,
  LINK,
  EMPHASIS
}
