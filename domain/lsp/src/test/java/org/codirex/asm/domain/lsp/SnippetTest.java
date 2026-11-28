package org.codirex.asm.domain.lsp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SnippetTest {

  @Test
  public void plainTextStaysAsItIs() {
    Snippet s = Snippet.parse("onStart()");
    assertEquals("onStart()", s.text());
    assertEquals(-1, s.caret());
  }

  @Test
  public void placeholdersBecomeTheirDefaultsAndTheFirstStopIsTheCaret() {
    Snippet s =
        Snippet.parse(
            "@Override\nprotected void onCreate(${2:Bundle savedInstanceState}) {\n\t${1:super.onCreate(savedInstanceState);}\n\t$0\n}");
    assertEquals(
        "@Override\nprotected void onCreate(Bundle savedInstanceState) {\n\tsuper.onCreate(savedInstanceState);\n\t\n}",
        s.text());
    assertEquals(s.text().indexOf("super.onCreate(savedInstanceState);"), s.caret());
  }

  @Test
  public void finalStopIsUsedWhenThereAreNoOthers() {
    Snippet s = Snippet.parse("foo($0)");
    assertEquals("foo()", s.text());
    assertEquals(4, s.caret());
  }

  @Test
  public void choicesEscapesAndNestedBraces() {
    assertEquals("a", Snippet.parse("${1|a,b|}").text());
    assertEquals("$x}", Snippet.parse("\\$x\\}").text());
    assertEquals("{x}", Snippet.parse("${1:{x}}").text());
  }

  @Test
  public void brokenPlaceholderKeepsTheTextAsItWas() {
    assertEquals("${1:oops", Snippet.parse("${1:oops").text());
  }
}
