package org.codirex.asm.domain.editor.highlight;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public final class JsonLexerTest {
  private final Lexer lexer = new JsonLexer();

  @Test
  public void keysAreNotValues() {
    assertEquals(
        Arrays.asList("\"a\":ATTRIBUTE_NAME", "\"b\":STRING"), Tokens.show(lexer, "{\"a\": \"b\"}"));
  }

  @Test
  public void numbersAndLiterals() {
    assertEquals(
        Arrays.asList("1:NUMBER", "-2.5e+3:NUMBER", "true:KEYWORD", "false:KEYWORD", "null:KEYWORD"),
        Tokens.show(lexer, "[1, -2.5e+3, true, false, null]"));
  }

  @Test
  public void escapesStayInsideTheString() {
    assertEquals(Arrays.asList("\"a\\\"b\":STRING"), Tokens.show(lexer, "\"a\\\"b\""));
  }

  @Test
  public void commentsAreAllowed() {
    assertEquals(
        Arrays.asList("// one:COMMENT", "1:NUMBER", "/* two\n */:COMMENT"),
        Tokens.show(lexer, "// one\n1 /* two\n */"));
  }

  @Test
  public void anUnclosedStringEndsWithTheLine() {
    assertEquals(
        Arrays.asList("\"open:STRING", "7:NUMBER"), Tokens.show(lexer, "\"open\n7"));
  }

  @Test
  public void otherWordsAreNotKeywords() {
    assertEquals(Arrays.asList(), Tokens.show(lexer, "nullable truest"));
  }
}
