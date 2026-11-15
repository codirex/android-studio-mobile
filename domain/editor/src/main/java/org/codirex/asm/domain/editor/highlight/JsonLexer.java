package org.codirex.asm.domain.editor.highlight;

import java.util.ArrayList;
import java.util.List;

/** JSON, with the comments that JSON with comments (settings files, tsconfig) allows. */
public final class JsonLexer implements Lexer {

  @Override
  public List<Token> tokenize(CharSequence text) {
    List<Token> out = new ArrayList<>();
    int n = text.length();
    int i = 0;
    while (i < n) {
      char c = text.charAt(i);
      if (c == '"') {
        int end = Scan.stringEnd(text, i);
        int next = end;
        while (next < n && Character.isWhitespace(text.charAt(next))) {
          next++;
        }
        boolean key = next < n && text.charAt(next) == ':';
        out.add(new Token(i, end, key ? TokenKind.ATTRIBUTE_NAME : TokenKind.STRING));
        i = end;
      } else if (Scan.startsWith(text, i, "//")) {
        int end = Scan.lineEnd(text, i);
        out.add(new Token(i, end, TokenKind.COMMENT));
        i = end;
      } else if (Scan.startsWith(text, i, "/*")) {
        int close = Scan.indexOf(text, "*/", i + 2);
        int end = close < 0 ? n : close + 2;
        out.add(new Token(i, end, TokenKind.COMMENT));
        i = end;
      } else if (isDigit(c) || (c == '-' && i + 1 < n && isDigit(text.charAt(i + 1)))) {
        int end = i + 1;
        while (end < n && isNumberChar(text.charAt(end))) {
          end++;
        }
        out.add(new Token(i, end, TokenKind.NUMBER));
        i = end;
      } else if (Character.isLetter(c)) {
        int end = i + 1;
        while (end < n && Character.isLetter(text.charAt(end))) {
          end++;
        }
        String word = text.subSequence(i, end).toString();
        if (word.equals("true") || word.equals("false") || word.equals("null")) {
          out.add(new Token(i, end, TokenKind.KEYWORD));
        }
        i = end;
      } else {
        i++;
      }
    }
    return out;
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isNumberChar(char c) {
    return isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-';
  }
}
