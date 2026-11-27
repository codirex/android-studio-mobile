package org.codirex.asm.domain.lsp;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class WorkspaceSymbolsTest {

  private static Map<String, Object> symbol(String name, long kind, String container, Object location) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", name);
    map.put("kind", kind);
    map.put("containerName", container);
    map.put("location", location);
    return map;
  }

  private static Map<String, Object> location(String uri, Long line, Long character) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("uri", uri);
    if (line != null) {
      Map<String, Object> start = new LinkedHashMap<>();
      start.put("line", line);
      start.put("character", character);
      Map<String, Object> range = new LinkedHashMap<>();
      range.put("start", start);
      range.put("end", start);
      map.put("range", range);
    }
    return map;
  }

  @Test
  public void symbolInformationAndWorkspaceSymbolBothRead() {
    List<WorkspaceSymbol> found =
        LanguageSession.workspaceSymbolsOf(
            Arrays.asList(
                symbol("MainActivity", 5L, "org.example", location("file:///p/Main.java", 9L, 13L)),
                symbol("run", 6L, "MainActivity", location("file:///p/Main.java", null, null)),
                "junk",
                symbol("lost", 6L, "", "nowhere")));
    assertEquals(2, found.size());
    assertEquals("MainActivity", found.get(0).name());
    assertEquals("org.example", found.get(0).container());
    assertEquals(9, found.get(0).line());
    assertEquals(13, found.get(0).character());
    assertEquals(0, found.get(1).line());
    assertEquals("file:///p/Main.java", found.get(1).uri());
  }

  @Test
  public void anythingElseIsEmpty() {
    assertEquals(Collections.emptyList(), LanguageSession.workspaceSymbolsOf(null));
    assertEquals(Collections.emptyList(), LanguageSession.workspaceSymbolsOf("x"));
  }
}
