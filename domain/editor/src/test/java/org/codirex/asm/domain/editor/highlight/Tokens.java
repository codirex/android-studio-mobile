package org.codirex.asm.domain.editor.highlight;

import java.util.ArrayList;
import java.util.List;

/** Test helper: shows tokens as {@code text:KIND} so that expectations are easy to read. */
final class Tokens {
  private Tokens() {}

  static List<String> show(Lexer lexer, String text) {
    List<String> shown = new ArrayList<>();
    int previousEnd = 0;
    for (Token token : lexer.tokenize(text)) {
      if (token.start() < previousEnd || token.end() > text.length()) {
        throw new AssertionError("Overlapping or out of range token " + token);
      }
      previousEnd = token.end();
      shown.add(text.substring(token.start(), token.end()) + ":" + token.kind());
    }
    return shown;
  }
}
