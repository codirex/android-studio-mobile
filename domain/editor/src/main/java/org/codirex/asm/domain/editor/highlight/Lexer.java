package org.codirex.asm.domain.editor.highlight;

import java.util.List;

/**
 * Splits a whole text into highlighted pieces. A lexer never fails: text it does not understand
 * (including text that is half typed) simply has no token. The tokens come in order, do not overlap,
 * and may span several lines.
 */
public interface Lexer {
  List<Token> tokenize(CharSequence text);
}
