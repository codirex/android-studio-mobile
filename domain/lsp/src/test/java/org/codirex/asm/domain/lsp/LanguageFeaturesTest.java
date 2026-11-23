package org.codirex.asm.domain.lsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public final class LanguageFeaturesTest {

  private static Object json(String text) {
    try {
      return Json.parse(text);
    } catch (JsonException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void hoverReadsMarkedStringsAndMarkupAndDropsFences() {
    String answer =
        LanguageSession.hoverOf(
            json(
                "{\"contents\":[{\"language\":\"java\",\"value\":\"void println(String x)\"},"
                    + "\"Prints a line.\\n\\n```java\\nfoo\\n```\"]}"));
    assertEquals("void println(String x)\n\nPrints a line.\n\nfoo", answer);
    assertEquals("text", LanguageSession.hoverOf(json("{\"contents\":{\"kind\":\"markdown\",\"value\":\"text\"}}")));
    assertEquals("plain", LanguageSession.hoverOf(json("{\"contents\":\"plain\"}")));
  }

  @Test
  public void hoverOfNothingIsEmpty() {
    assertEquals("", LanguageSession.hoverOf(null));
    assertEquals("", LanguageSession.hoverOf(json("{\"contents\":[]}")));
  }

  @Test
  public void locationsReadLocationsAndLinks() {
    List<Location> one =
        LanguageSession.locationsOf(
            json("{\"uri\":\"file:///A.java\",\"range\":{\"start\":{\"line\":4,\"character\":2},\"end\":{\"line\":4,\"character\":9}}}"));
    assertEquals(new Location("file:///A.java", 4, 2), one.get(0));
    List<Location> links =
        LanguageSession.locationsOf(
            json("[{\"targetUri\":\"file:///B.java\",\"targetRange\":{\"start\":{\"line\":1,\"character\":0},\"end\":{\"line\":9,\"character\":0}},"
                + "\"targetSelectionRange\":{\"start\":{\"line\":3,\"character\":6},\"end\":{\"line\":3,\"character\":8}}},{\"junk\":1}]"));
    assertEquals(1, links.size());
    assertEquals(new Location("file:///B.java", 3, 6), links.get(0));
    assertTrue(LanguageSession.locationsOf(null).isEmpty());
  }

  @Test
  public void editsComeLastPositionFirst() {
    List<TextEdit> edits =
        LanguageSession.editsOf(
            json("[{\"range\":{\"start\":{\"line\":0,\"character\":1},\"end\":{\"line\":0,\"character\":2}},\"newText\":\"a\"},"
                + "{\"range\":{\"start\":{\"line\":5,\"character\":0},\"end\":{\"line\":5,\"character\":0}},\"newText\":\"b\"},"
                + "{\"range\":{\"start\":{\"line\":0,\"character\":7},\"end\":{\"line\":1,\"character\":0}},\"newText\":\"c\"},"
                + "{\"newText\":\"no range\"}]"));
    assertEquals(3, edits.size());
    assertEquals("b", edits.get(0).newText());
    assertEquals("c", edits.get(1).newText());
    assertEquals("a", edits.get(2).newText());
  }
}
