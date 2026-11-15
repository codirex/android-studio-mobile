package org.codirex.asm.domain.editor.highlight;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown: headings, fenced code blocks, quotes, list markers, rules, and inline code, links and
 * strong emphasis. Nothing is rendered; this is only colour for the source text.
 */
public final class MarkdownLexer implements Lexer {

  @Override
  public List<Token> tokenize(CharSequence text) {
    List<Token> out = new ArrayList<>();
    int n = text.length();
    int lineStart = 0;
    boolean inFence = false;
    char fenceChar = 0;
    int fenceLength = 0;
    while (lineStart <= n) {
      int end = Scan.lineEnd(text, lineStart);
      int indent = lineStart;
      while (indent < end && indent - lineStart < 3 && text.charAt(indent) == ' ') {
        indent++;
      }
      if (inFence) {
        add(out, lineStart, end, TokenKind.CODE);
        if (closesFence(text, indent, end, fenceChar, fenceLength)) {
          inFence = false;
        }
      } else if (isFenceStart(text, indent, end)) {
        inFence = true;
        fenceChar = text.charAt(indent);
        fenceLength = run(text, indent, end, fenceChar);
        add(out, lineStart, end, TokenKind.CODE);
      } else if (isHeading(text, indent, end)) {
        add(out, indent, end, TokenKind.HEADING);
      } else if (isRule(text, indent, end)) {
        add(out, indent, end, TokenKind.OPERATOR);
      } else {
        int from = indent;
        if (from < end && text.charAt(from) == '>') {
          out.add(new Token(from, from + 1, TokenKind.OPERATOR));
          from++;
        } else {
          int marker = listMarkerEnd(text, from, end);
          if (marker > from) {
            out.add(new Token(from, marker, TokenKind.OPERATOR));
            from = marker;
          }
        }
        inline(text, from, end, out);
      }
      if (end >= n) {
        break;
      }
      lineStart = end + 1;
      if (text.charAt(end) == '\r' && lineStart < n && text.charAt(lineStart) == '\n') {
        lineStart++;
      }
    }
    return out;
  }

  private static void add(List<Token> out, int start, int end, TokenKind kind) {
    if (end > start) {
      out.add(new Token(start, end, kind));
    }
  }

  private static int run(CharSequence text, int from, int end, char c) {
    int i = from;
    while (i < end && text.charAt(i) == c) {
      i++;
    }
    return i - from;
  }

  private static boolean isFenceStart(CharSequence text, int at, int end) {
    if (at >= end) {
      return false;
    }
    char c = text.charAt(at);
    return (c == '`' || c == '~') && run(text, at, end, c) >= 3;
  }

  private static boolean closesFence(CharSequence text, int at, int end, char fenceChar, int length) {
    if (at >= end || text.charAt(at) != fenceChar) {
      return false;
    }
    int count = run(text, at, end, fenceChar);
    if (count < length) {
      return false;
    }
    for (int i = at + count; i < end; i++) {
      if (!Character.isWhitespace(text.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isHeading(CharSequence text, int at, int end) {
    int hashes = run(text, at, end, '#');
    return hashes >= 1 && hashes <= 6 && (at + hashes == end || text.charAt(at + hashes) == ' ');
  }

  /** Three or more of the same of {@code - * _}, with nothing else but spaces. */
  private static boolean isRule(CharSequence text, int at, int end) {
    if (at >= end) {
      return false;
    }
    char c = text.charAt(at);
    if (c != '-' && c != '*' && c != '_') {
      return false;
    }
    int count = 0;
    for (int i = at; i < end; i++) {
      char d = text.charAt(i);
      if (d == c) {
        count++;
      } else if (d != ' ') {
        return false;
      }
    }
    return count >= 3;
  }

  /** The end of a list marker ({@code -}, {@code *}, {@code +}, {@code 1.}, {@code 1)}), or {@code at}. */
  private static int listMarkerEnd(CharSequence text, int at, int end) {
    if (at >= end) {
      return at;
    }
    char c = text.charAt(at);
    if ((c == '-' || c == '*' || c == '+') && at + 1 < end && text.charAt(at + 1) == ' ') {
      return at + 1;
    }
    int i = at;
    while (i < end && text.charAt(i) >= '0' && text.charAt(i) <= '9') {
      i++;
    }
    if (i > at && i < end - 1 && (text.charAt(i) == '.' || text.charAt(i) == ')') && text.charAt(i + 1) == ' ') {
      return i + 1;
    }
    return at;
  }

  private static void inline(CharSequence text, int from, int end, List<Token> out) {
    int i = from;
    while (i < end) {
      char c = text.charAt(i);
      if (c == '\\') {
        i += 2;
      } else if (c == '`') {
        int ticks = run(text, i, end, '`');
        int close = closingTicks(text, i + ticks, end, ticks);
        if (close < 0) {
          i += ticks;
        } else {
          out.add(new Token(i, close + ticks, TokenKind.CODE));
          i = close + ticks;
        }
      } else if (c == '[' || (c == '!' && i + 1 < end && text.charAt(i + 1) == '[')) {
        int link = linkEnd(text, c == '!' ? i + 1 : i, end);
        if (link < 0) {
          i++;
        } else {
          out.add(new Token(i, link, TokenKind.LINK));
          i = link;
        }
      } else if ((c == '*' || c == '_') && i + 1 < end && text.charAt(i + 1) == c) {
        int close = strongEnd(text, i, end, c);
        if (close < 0) {
          i += 2;
        } else {
          out.add(new Token(i, close, TokenKind.EMPHASIS));
          i = close;
        }
      } else {
        i++;
      }
    }
  }

  /** The start of the next run of exactly {@code ticks} backticks, or -1. */
  private static int closingTicks(CharSequence text, int from, int end, int ticks) {
    int i = from;
    while (i < end) {
      if (text.charAt(i) == '`') {
        int length = run(text, i, end, '`');
        if (length == ticks) {
          return i;
        }
        i += length;
      } else {
        i++;
      }
    }
    return -1;
  }

  /** The end of {@code [text](target)} whose bracket is at {@code open}, or -1. */
  private static int linkEnd(CharSequence text, int open, int end) {
    int i = open + 1;
    while (i < end && text.charAt(i) != ']') {
      i++;
    }
    if (i + 1 >= end || text.charAt(i + 1) != '(') {
      return -1;
    }
    int j = i + 2;
    while (j < end && text.charAt(j) != ')') {
      j++;
    }
    return j < end ? j + 1 : -1;
  }

  /** The end of {@code **text**} that starts at {@code at}, or -1. */
  private static int strongEnd(CharSequence text, int at, int end, char c) {
    int i = at + 2;
    if (i >= end || text.charAt(i) == ' ') {
      return -1;
    }
    while (i + 1 < end) {
      if (text.charAt(i) == c && text.charAt(i + 1) == c && text.charAt(i - 1) != ' ') {
        return i + 2;
      }
      i++;
    }
    return -1;
  }
}
