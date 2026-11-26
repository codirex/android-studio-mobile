package org.codirex.asm.domain.lsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class DocumentSymbolsTest {

  private static Map<String, Object> pos(long line, long character) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("line", line);
    map.put("character", character);
    return map;
  }

  private static Map<String, Object> range(long line, long character) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("start", pos(line, character));
    map.put("end", pos(line, character + 3));
    return map;
  }

  @Test
  public void aHierarchicalAnswerIsFlattenedInOrderWithTheContainer() {
    Map<String, Object> method = new LinkedHashMap<>();
    method.put("name", "run");
    method.put("detail", "()");
    method.put("kind", 6L);
    method.put("range", range(2, 2));
    method.put("selectionRange", range(2, 16));
    Map<String, Object> type = new LinkedHashMap<>();
    type.put("name", "Main");
    type.put("kind", 5L);
    type.put("range", range(0, 0));
    type.put("selectionRange", range(0, 13));
    type.put("children", Collections.singletonList(method));

    List<DocumentSymbol> symbols = LanguageSession.symbolsOf(Collections.singletonList(type));

    assertEquals(2, symbols.size());
    assertEquals(new DocumentSymbol("Main", "", 5, 0, 13, ""), symbols.get(0));
    assertEquals(new DocumentSymbol("run", "()", 6, 2, 16, "Main"), symbols.get(1));
    assertEquals("class", symbols.get(0).kindName());
    assertEquals("method", symbols.get(1).kindName());
  }

  @Test
  public void aFlatAnswerUsesTheLocationAndTheContainerName() {
    Map<String, Object> location = new LinkedHashMap<>();
    location.put("uri", "file:///a/Main.java");
    location.put("range", range(7, 4));
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("name", "count");
    info.put("kind", 8L);
    info.put("containerName", "Main");
    info.put("location", location);

    List<DocumentSymbol> symbols = LanguageSession.symbolsOf(Collections.singletonList(info));

    assertEquals(Arrays.asList(new DocumentSymbol("count", "", 8, 7, 4, "Main")), symbols);
  }

  @Test
  public void somethingElseGivesNothing() {
    assertTrue(LanguageSession.symbolsOf(null).isEmpty());
    assertTrue(LanguageSession.symbolsOf("x").isEmpty());
    Map<String, Object> noName = new LinkedHashMap<>();
    noName.put("kind", 5L);
    noName.put("range", range(0, 0));
    List<Object> entries = new ArrayList<>();
    entries.add(noName);
    entries.add("text");
    assertTrue(LanguageSession.symbolsOf(entries).isEmpty());
  }

  @Test
  public void anUnknownKindIsJustASymbol() {
    assertEquals("symbol", new DocumentSymbol("x", "", 99, 0, 0, "").kindName());
  }
}
