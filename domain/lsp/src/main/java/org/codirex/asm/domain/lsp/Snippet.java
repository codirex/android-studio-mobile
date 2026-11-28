package org.codirex.asm.domain.lsp;

/**
 * A snippet of the protocol ({@code insertTextFormat} 2) turned into plain text: tab stops and
 * placeholders become their default text, and the first tab stop (or {@code $0}) tells where the
 * caret goes. Immutable.
 */
public final class Snippet {

  private final String text;
  private final int caret;

  private Snippet(String text, int caret) {
    this.text = text;
    this.caret = caret;
  }

  /** The text to insert. */
  public String text() {
    return text;
  }

  /** Where the caret goes, counted in characters of {@link #text()}; -1 if the snippet says nothing. */
  public int caret() {
    return caret;
  }

  /** Reads {@code snippet}; text that is no snippet comes back as it is. */
  public static Snippet parse(String snippet) {
    StringBuilder out = new StringBuilder();
    int[] first = {Integer.MAX_VALUE, -1}; // lowest tab stop number and its place
    int end = -1; // the place of $0
    int i = 0;
    int n = snippet.length();
    while (i < n) {
      char c = snippet.charAt(i);
      if (c == '\\' && i + 1 < n && "$}\\".indexOf(snippet.charAt(i + 1)) >= 0) {
        out.append(snippet.charAt(i + 1));
        i += 2;
      } else if (c == '$' && i + 1 < n) {
        char d = snippet.charAt(i + 1);
        if (Character.isDigit(d)) {
          int j = i + 1;
          while (j < n && Character.isDigit(snippet.charAt(j))) {
            j++;
          }
          stop(Integer.parseInt(snippet.substring(i + 1, j)), out.length(), first);
          if (snippet.substring(i + 1, j).equals("0")) {
            end = out.length();
          }
          i = j;
        } else if (d == '{') {
          i = placeholder(snippet, i + 2, out, first);
          if (i < 0) {
            return new Snippet(snippet, -1);
          }
        } else {
          out.append(c);
          i++;
        }
      } else {
        out.append(c);
        i++;
      }
    }
    int caret = first[1] >= 0 ? first[1] : end;
    return new Snippet(out.toString(), caret);
  }

  private static void stop(int number, int place, int[] first) {
    if (number > 0 && number < first[0]) {
      first[0] = number;
      first[1] = place;
    }
  }

  /** Reads {@code ${n}}, {@code ${n:text}} or {@code ${n|a,b|}} after the brace; the index after it, or -1. */
  private static int placeholder(String s, int from, StringBuilder out, int[] first) {
    int n = s.length();
    int j = from;
    while (j < n && Character.isDigit(s.charAt(j))) {
      j++;
    }
    if (j == from || j >= n) {
      return -1;
    }
    int number = Integer.parseInt(s.substring(from, j));
    stop(number, out.length(), first);
    char c = s.charAt(j);
    if (c == '}') {
      return j + 1;
    }
    if (c == '|') {
      int bar = s.indexOf('|', j + 1);
      if (bar < 0 || bar + 1 >= n || s.charAt(bar + 1) != '}') {
        return -1;
      }
      String choices = s.substring(j + 1, bar);
      int comma = choices.indexOf(',');
      out.append(comma < 0 ? choices : choices.substring(0, comma));
      return bar + 2;
    }
    if (c != ':') {
      return -1;
    }
    int k = j + 1;
    int depth = 1;
    while (k < n) {
      char d = s.charAt(k);
      if (d == '\\' && k + 1 < n) {
        out.append(s.charAt(k + 1));
        k += 2;
        continue;
      }
      if (d == '{') {
        depth++;
      } else if (d == '}') {
        depth--;
        if (depth == 0) {
          return k + 1;
        }
      }
      out.append(d);
      k++;
    }
    return -1;
  }
}
