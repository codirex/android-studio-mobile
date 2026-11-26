package org.codirex.asm.domain.palette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Matches what a person types against names, the way the command palette needs: every typed letter
 * must appear in the name in the same order (not next to each other), upper and lower case are the
 * same, and spaces in the query are ignored. A better match scores higher: letters that follow each
 * other, letters that start a word (after a slash, dot, dash, underscore or at a camel-case hump),
 * a match in the file name rather than the folders, and a shorter name.
 */
public final class FuzzyMatcher {

  private FuzzyMatcher() {}

  /** The score of {@code text} for {@code query}: -1 if it does not match, otherwise more is better. */
  public static int score(String query, String text) {
    String q = query.replace(" ", "");
    if (q.isEmpty()) {
      return 0;
    }
    int next = 0;
    int score = 0;
    int last = -2;
    for (int i = 0; i < text.length() && next < q.length(); i++) {
      char c = text.charAt(i);
      if (Character.toLowerCase(c) != Character.toLowerCase(q.charAt(next))) {
        continue;
      }
      int points = 1;
      if (i == last + 1) {
        points += 5;
      }
      if (i == 0 || startsWord(text, i)) {
        points += 4;
      }
      score += points;
      last = i;
      next++;
    }
    if (next < q.length()) {
      return -1;
    }
    String name = text.substring(text.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
    String lower = q.toLowerCase(Locale.ROOT);
    if (name.startsWith(lower)) {
      score += 20;
    } else if (name.contains(lower)) {
      score += 10;
    }
    return score * 100 - Math.min(text.length(), 99);
  }

  private static boolean startsWord(String text, int index) {
    char before = text.charAt(index - 1);
    if (before == '/' || before == '.' || before == '-' || before == '_' || before == ' ') {
      return true;
    }
    return Character.isUpperCase(text.charAt(index)) && Character.isLowerCase(before);
  }

  /**
   * The items that match {@code query}, best first (items with the same score keep their order), at
   * most {@code limit}. An empty query matches everything and keeps the order.
   */
  public static <T> List<T> rank(
      String query, List<T> items, Function<T, String> label, int limit) {
    List<T> out = new ArrayList<>();
    if (query.trim().isEmpty()) {
      for (T item : items) {
        if (out.size() >= limit) {
          break;
        }
        out.add(item);
      }
      return out;
    }
    List<int[]> scored = new ArrayList<>();
    for (int i = 0; i < items.size(); i++) {
      int score = score(query, label.apply(items.get(i)));
      if (score >= 0) {
        scored.add(new int[] {score, i});
      }
    }
    Collections.sort(
        scored,
        (a, b) -> a[0] != b[0] ? Integer.compare(b[0], a[0]) : Integer.compare(a[1], b[1]));
    for (int[] entry : scored) {
      if (out.size() >= limit) {
        break;
      }
      out.add(items.get(entry[1]));
    }
    return out;
  }
}
