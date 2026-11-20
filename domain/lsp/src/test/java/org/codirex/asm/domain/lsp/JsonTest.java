package org.codirex.asm.domain.lsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public final class JsonTest {

  @Test
  public void valuesOfEveryKindAreRead() throws Exception {
    Map<?, ?> map =
        (Map<?, ?>)
            Json.parse(
                "{\"a\":1,\"b\":-2.5,\"c\":[true,false,null],\"d\":\"x\\n\\u0041\\\"\",\"e\":{},\"f\":1e3}");
    assertEquals(1L, map.get("a"));
    assertEquals(-2.5, (Double) map.get("b"), 0);
    assertEquals(3, ((List<?>) map.get("c")).size());
    assertEquals("x\nA\"", map.get("d"));
    assertTrue(((Map<?, ?>) map.get("e")).isEmpty());
    assertEquals(1000.0, (Double) map.get("f"), 0);
  }

  @Test
  public void aWrittenValueReadsBackTheSame() throws Exception {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("text", "tab\t quote\" back\\ ctl\u0001 uni\u00e9");
    value.put("n", 42L);
    value.put("list", java.util.Arrays.asList(1L, "two", null, true));
    assertEquals(value, Json.parse(Json.write(value)));
  }

  @Test
  public void badTextIsRefused() {
    String[] bad = {"", "{", "[1,]", "{\"a\"}", "tru", "\"abc", "1 2", "{\"a\":01x}", "-", "\"\\q\""};
    for (String text : bad) {
      try {
        Json.parse(text);
        fail("parsed: " + text);
      } catch (JsonException expected) {
        // expected
      }
    }
  }

  @Test
  public void tooDeepNestingIsRefusedNotAStackOverflow() {
    StringBuilder deep = new StringBuilder();
    for (int i = 0; i < 5000; i++) {
      deep.append('[');
    }
    try {
      Json.parse(deep.toString());
      fail("parsed");
    } catch (JsonException expected) {
      // expected
    }
  }

  @Test
  public void nonFiniteNumbersAndForeignObjectsAreNotWritten() {
    try {
      Json.write(Double.NaN);
      fail("written");
    } catch (JsonException expected) {
      // expected
    }
    try {
      Json.write(new Object());
      fail("written");
    } catch (JsonException expected) {
      // expected
    }
  }

  @Test
  public void nullIsNull() throws Exception {
    assertNull(Json.parse(" null "));
  }
}
