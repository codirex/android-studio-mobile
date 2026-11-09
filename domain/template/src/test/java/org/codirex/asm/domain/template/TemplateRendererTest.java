package org.codirex.asm.domain.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import org.codirex.asm.domain.template.TemplateException.Reason;
import org.junit.Test;

public final class TemplateRendererTest {

  private static Map<String, String> vars() {
    Map<String, String> map = new HashMap<>();
    map.put("name", "Demo");
    map.put("pkg", "org.codirex.demo");
    map.put("empty", "");
    return map;
  }

  private static String render(String text) throws TemplateException {
    return TemplateRenderer.render(text, vars());
  }

  @Test
  public void replacesPlaceholders() throws Exception {
    assertEquals("package org.codirex.demo; // Demo", render("package ${pkg}; // ${name}"));
  }

  @Test
  public void replacesTheSamePlaceholderEverywhere() throws Exception {
    assertEquals("Demo-Demo", render("${name}-${name}"));
  }

  @Test
  public void anEmptyValueIsFine() throws Exception {
    assertEquals("ab", render("a${empty}b"));
  }

  @Test
  public void textWithoutPlaceholdersIsUnchanged() throws Exception {
    assertEquals("plain text\nwith lines\n", render("plain text\nwith lines\n"));
    assertEquals("", render(""));
  }

  @Test
  public void aLoneDollarSignStays() throws Exception {
    assertEquals("echo $HOME and $name and 5$", render("echo $HOME and $name and 5$"));
  }

  @Test
  public void twoDollarSignsWriteOne() throws Exception {
    assertEquals("${name} stays", render("$${name} stays"));
    assertEquals("$", render("$$"));
    assertEquals("$Demo", render("$$${name}"));
  }

  @Test
  public void anUnknownVariableFails() {
    assertFails(Reason.UNKNOWN_VARIABLE, "hello ${nobody}");
    assertFails(Reason.UNKNOWN_VARIABLE, "${}");
    assertFails(Reason.UNKNOWN_VARIABLE, "${Name}");
  }

  @Test
  public void anUnclosedPlaceholderFails() {
    assertFails(Reason.MALFORMED, "hello ${name");
    assertFails(Reason.MALFORMED, "${");
  }

  @Test
  public void aValueIsNotRenderedAgain() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("a", "${b}");
    map.put("b", "B");

    assertEquals("${b}", TemplateRenderer.render("${a}", map));
  }

  private static void assertFails(Reason expected, String text) {
    try {
      render(text);
      fail("expected " + expected);
    } catch (TemplateException e) {
      assertEquals(expected, e.reason());
    }
  }
}
