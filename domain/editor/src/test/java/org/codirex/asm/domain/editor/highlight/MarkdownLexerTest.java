package org.codirex.asm.domain.editor.highlight;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public final class MarkdownLexerTest {
  private final Lexer lexer = new MarkdownLexer();

  @Test
  public void headings() {
    assertEquals(
        Arrays.asList("# Title:HEADING", "### Sub:HEADING"), Tokens.show(lexer, "# Title\ntext\n### Sub"));
  }

  @Test
  public void aHashWithoutSpaceIsNotAHeading() {
    assertEquals(Arrays.asList(), Tokens.show(lexer, "#tag\n####### seven"));
  }

  @Test
  public void fencedCodeKeepsEverythingInside() {
    assertEquals(
        Arrays.asList("```java:CODE", "# not a heading:CODE", "```:CODE"),
        Tokens.show(lexer, "```java\n# not a heading\n```\nafter"));
  }

  @Test
  public void anUnclosedFenceRunsToTheEnd() {
    assertEquals(Arrays.asList("~~~:CODE", "x:CODE"), Tokens.show(lexer, "~~~\nx"));
  }

  @Test
  public void aLongerFenceNeedsALongerClose() {
    assertEquals(
        Arrays.asList("````:CODE", "```:CODE", "````:CODE"), Tokens.show(lexer, "````\n```\n````"));
  }

  @Test
  public void listsQuotesAndRules() {
    assertEquals(
        Arrays.asList("-:OPERATOR", "1.:OPERATOR", ">:OPERATOR", "---:OPERATOR", "* * *:OPERATOR"),
        Tokens.show(lexer, "- a\n1. b\n> c\n---\n* * *"));
  }

  @Test
  public void inlineCodeLinksAndStrong() {
    assertEquals(
        Arrays.asList("`a*`:CODE", "[x](http://y):LINK", "**bold**:EMPHASIS", "![i](p.png):LINK"),
        Tokens.show(lexer, "`a*` [x](http://y) **bold** ![i](p.png)"));
  }

  @Test
  public void unfinishedInlineMarksAreText() {
    assertEquals(Arrays.asList(), Tokens.show(lexer, "`open [no](link **no"));
  }

  @Test
  public void windowsLineBreaks() {
    assertEquals(
        Arrays.asList("# A:HEADING", "-:OPERATOR"), Tokens.show(lexer, "# A\r\n- b\r\n"));
  }

  @Test
  public void emptyText() {
    assertEquals(Arrays.asList(), Tokens.show(lexer, ""));
  }
}
