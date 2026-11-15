package org.codirex.asm.domain.editor.highlight;

import java.util.ArrayList;
import java.util.List;

/** XML: tags, attributes and their values, comments, CDATA, processing instructions. */
public final class XmlLexer implements Lexer {

  @Override
  public List<Token> tokenize(CharSequence text) {
    List<Token> out = new ArrayList<>();
    int n = text.length();
    int i = 0;
    while (i < n) {
      if (text.charAt(i) != '<') {
        i++;
      } else if (Scan.startsWith(text, i, "<!--")) {
        i = add(out, i, Scan.indexOf(text, "-->", i + 4), 3, n, TokenKind.COMMENT);
      } else if (Scan.startsWith(text, i, "<![CDATA[")) {
        i = add(out, i, Scan.indexOf(text, "]]>", i + 9), 3, n, TokenKind.STRING);
      } else if (Scan.startsWith(text, i, "<?")) {
        i = add(out, i, Scan.indexOf(text, "?>", i + 2), 2, n, TokenKind.KEYWORD);
      } else if (Scan.startsWith(text, i, "<!")) {
        i = add(out, i, Scan.indexOf(text, ">", i + 2), 1, n, TokenKind.KEYWORD);
      } else {
        i = tag(text, i, out);
      }
    }
    return out;
  }

  /** Adds a token that ends with a closing marker (or at the end of the text) and returns its end. */
  private static int add(List<Token> out, int start, int close, int closeLength, int n, TokenKind kind) {
    int end = close < 0 ? n : close + closeLength;
    out.add(new Token(start, end, kind));
    return end;
  }

  /** Reads a tag that starts at {@code start}; returns where to go on. */
  private static int tag(CharSequence text, int start, List<Token> out) {
    int n = text.length();
    int i = start + 1;
    if (i < n && text.charAt(i) == '/') {
      i++;
    }
    int nameStart = i;
    while (i < n && isNameChar(text.charAt(i))) {
      i++;
    }
    if (i == nameStart) {
      return start + 1; // A lone "<" is just text.
    }
    out.add(new Token(start, i, TokenKind.TAG));
    while (i < n) {
      char c = text.charAt(i);
      if (c == '>') {
        out.add(new Token(i, i + 1, TokenKind.TAG));
        return i + 1;
      }
      if (c == '/' && i + 1 < n && text.charAt(i + 1) == '>') {
        out.add(new Token(i, i + 2, TokenKind.TAG));
        return i + 2;
      }
      if (c == '<') {
        return i; // A tag that was never closed; the next one starts here.
      }
      if (c == '"' || c == '\'') {
        int close = Scan.indexOf(text, String.valueOf(c), i + 1);
        int end = close < 0 ? Scan.lineEnd(text, i) : close + 1;
        out.add(new Token(i, end, TokenKind.ATTRIBUTE_VALUE));
        i = end;
      } else if (c == '=') {
        out.add(new Token(i, i + 1, TokenKind.OPERATOR));
        i++;
      } else if (isNameChar(c)) {
        int from = i;
        while (i < n && isNameChar(text.charAt(i))) {
          i++;
        }
        out.add(new Token(from, i, TokenKind.ATTRIBUTE_NAME));
      } else {
        i++;
      }
    }
    return n;
  }

  private static boolean isNameChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':';
  }
}
