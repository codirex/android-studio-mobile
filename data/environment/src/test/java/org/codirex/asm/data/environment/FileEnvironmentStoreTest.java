package org.codirex.asm.data.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codirex.asm.domain.environment.EnvironmentVariable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class FileEnvironmentStoreTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private static Map<String, String> asMap(List<EnvironmentVariable> list) {
    Map<String, String> map = new HashMap<>();
    for (EnvironmentVariable variable : list) {
      map.put(variable.name(), variable.value());
    }
    return map;
  }

  @Test
  public void aMissingFileIsNoVariables() throws Exception {
    assertTrue(new FileEnvironmentStore(new File(tmp.getRoot(), "none.properties")).load().isEmpty());
  }

  @Test
  public void variablesSurviveARoundTripWithAwkwardValues() throws Exception {
    File file = new File(tmp.getRoot(), "sub/environment.properties");
    FileEnvironmentStore store = new FileEnvironmentStore(file);
    List<EnvironmentVariable> written = new ArrayList<>();
    written.add(new EnvironmentVariable("PLAIN", "value"));
    written.add(new EnvironmentVariable("EQUALS", "a=b:c"));
    written.add(new EnvironmentVariable("SLASHES", "C:\\dir\\file"));
    written.add(new EnvironmentVariable("MULTI", "line1\nline2\ttab"));
    written.add(new EnvironmentVariable("ARABIC", "مرحبا"));
    written.add(new EnvironmentVariable("SPACES", "  padded  "));
    written.add(new EnvironmentVariable("EMPTY", ""));
    store.save(written);
    assertEquals(asMap(written), asMap(store.load()));
    assertFalse(new File(file.getPath() + ".tmp").exists());
  }

  @Test
  public void savingAgainReplacesTheContent() throws Exception {
    FileEnvironmentStore store = new FileEnvironmentStore(new File(tmp.getRoot(), "e.properties"));
    List<EnvironmentVariable> first = new ArrayList<>();
    first.add(new EnvironmentVariable("A", "1"));
    first.add(new EnvironmentVariable("B", "2"));
    store.save(first);
    List<EnvironmentVariable> second = new ArrayList<>();
    second.add(new EnvironmentVariable("B", "3"));
    store.save(second);
    Map<String, String> loaded = asMap(store.load());
    assertEquals(1, loaded.size());
    assertEquals("3", loaded.get("B"));
  }

  @Test
  public void entriesThatBreakTheRulesAreIgnored() throws Exception {
    File file = new File(tmp.getRoot(), "e.properties");
    Files.write(
        file.toPath(),
        "GOOD=1\nHOME=/evil\nBAD-NAME=2\n1ST=3\n".getBytes(StandardCharsets.UTF_8));
    Map<String, String> loaded = asMap(new FileEnvironmentStore(file).load());
    assertEquals(1, loaded.size());
    assertEquals("1", loaded.get("GOOD"));
  }
}
