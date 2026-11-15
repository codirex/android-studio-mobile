package org.codirex.asm.domain.editor.highlight;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public final class GradleLexerTest {
  private final Lexer lexer = new GradleLexer();

  @Test
  public void groovyDependencies() {
    assertEquals(
        Arrays.asList("dependencies:BUILTIN", "implementation:BUILTIN", "'a:b:1.0':STRING"),
        Tokens.show(lexer, "dependencies { implementation 'a:b:1.0' }"));
  }

  @Test
  public void kotlinDsl() {
    assertEquals(
        Arrays.asList("val:KEYWORD", "\"x\":STRING", "17:NUMBER", "plugins:BUILTIN"),
        Tokens.show(lexer, "val a = \"x\"; 17 plugins"));
  }

  @Test
  public void commentsAndTripleQuotes() {
    assertEquals(
        Arrays.asList("// c:COMMENT", "def:KEYWORD", "\"\"\"a\n\"b\"\"\":STRING", "/* d */:COMMENT"),
        Tokens.show(lexer, "// c\ndef \"\"\"a\n\"b\"\"\" /* d */"));
  }

  @Test
  public void namesThatContainKeywordsAreNotKeywords() {
    assertEquals(Arrays.asList(), Tokens.show(lexer, "defined variable value1 $it"));
  }

  @Test
  public void numbersWithSuffixes() {
    assertEquals(
        Arrays.asList("1_000L:NUMBER", "0.5f:NUMBER"), Tokens.show(lexer, "1_000L 0.5f"));
  }
}
