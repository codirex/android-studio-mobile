package org.codirex.asm.domain.editor.highlight;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public final class XmlLexerTest {
  private final Lexer lexer = new XmlLexer();

  @Test
  public void tagWithAttributes() {
    assertEquals(
        Arrays.asList(
            "<TextView:TAG",
            "android:id:ATTRIBUTE_NAME",
            "=:OPERATOR",
            "\"@+id/a\":ATTRIBUTE_VALUE",
            "/>:TAG"),
        Tokens.show(lexer, "<TextView android:id=\"@+id/a\"/>"));
  }

  @Test
  public void closingTagAndTextBetween() {
    assertEquals(
        Arrays.asList("<b:TAG", ">:TAG", "</b:TAG", ">:TAG"), Tokens.show(lexer, "<b>text</b>"));
  }

  @Test
  public void commentsSpanLines() {
    assertEquals(
        Arrays.asList("<!-- a\nb -->:COMMENT", "<x:TAG", "/>:TAG"),
        Tokens.show(lexer, "<!-- a\nb --><x/>"));
  }

  @Test
  public void declarationCdataAndDoctype() {
    assertEquals(
        Arrays.asList("<?xml version=\"1.0\"?>:KEYWORD", "<![CDATA[<x>]]>:STRING", "<!DOCTYPE a>:KEYWORD"),
        Tokens.show(lexer, "<?xml version=\"1.0\"?><![CDATA[<x>]]><!DOCTYPE a>"));
  }

  @Test
  public void aLoneAngleBracketIsText() {
    assertEquals(Arrays.asList(), Tokens.show(lexer, "1 < 2 and 3 > 2"));
  }

  @Test
  public void anUnclosedTagDoesNotSwallowTheNextOne() {
    assertEquals(
        Arrays.asList("<a:TAG", "b:ATTRIBUTE_NAME", "<c:TAG", "/>:TAG"),
        Tokens.show(lexer, "<a b\n<c/>"));
  }

  @Test
  public void anUnclosedValueEndsWithTheLine() {
    assertEquals(
        Arrays.asList("<a:TAG", "x:ATTRIBUTE_NAME", "=:OPERATOR", "\"abc:ATTRIBUTE_VALUE", "y:ATTRIBUTE_NAME"),
        Tokens.show(lexer, "<a x=\"abc\ny"));
  }

  @Test
  public void unclosedCommentRunsToTheEnd() {
    assertEquals(Arrays.asList("<!-- open\nstill:COMMENT"), Tokens.show(lexer, "<!-- open\nstill"));
  }

  @Test
  public void emptyText() {
    assertEquals(Arrays.asList(), Tokens.show(lexer, ""));
  }
}
