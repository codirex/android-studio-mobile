package org.codirex.asm.domain.palette;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class FuzzyMatcherTest {

  @Test
  public void lettersMustAppearInOrder() {
    assertEquals(-1, FuzzyMatcher.score("xyz", "abc"));
    assertEquals(-1, FuzzyMatcher.score("ba", "ab"));
    assertTrue(FuzzyMatcher.score("ac", "abc") >= 0);
  }

  @Test
  public void caseDoesNotMatterAndSpacesAreIgnored() {
    assertTrue(FuzzyMatcher.score("MAIN", "mainactivity") >= 0);
    assertTrue(FuzzyMatcher.score("ma in", "MainActivity") >= 0);
  }

  @Test
  public void camelCaseHumpsCanBeTyped() {
    assertTrue(FuzzyMatcher.score("mact", "MainActivity.java") >= 0);
  }

  @Test
  public void aNameThatStartsWithTheQueryBeatsOneThatOnlyContainsIt() {
    List<String> files =
        Arrays.asList("README.md", "app/src/domain/Remainder.java", "app/MainActivity.java");
    List<String> ranked = FuzzyMatcher.rank("main", files, s -> s, 10);
    assertEquals(Arrays.asList("app/MainActivity.java", "app/src/domain/Remainder.java"), ranked);
  }

  @Test
  public void aShorterNameWinsWhenTheMatchIsTheSame() {
    List<String> files = Arrays.asList("a/EditorFragmentTest.java", "a/EditorFragment.java");
    assertEquals("a/EditorFragment.java", FuzzyMatcher.rank("editor", files, s -> s, 10).get(0));
  }

  @Test
  public void anEmptyQueryKeepsTheOrderAndTheLimit() {
    List<String> items = Arrays.asList("c", "a", "b");
    assertEquals(items, FuzzyMatcher.rank("", items, s -> s, 10));
    assertEquals(Arrays.asList("c", "a"), FuzzyMatcher.rank("  ", items, s -> s, 2));
  }

  @Test
  public void theLimitCutsTheResult() {
    List<String> items = Arrays.asList("aa", "ab", "ac", "ad");
    assertEquals(2, FuzzyMatcher.rank("a", items, s -> s, 2).size());
  }
}
