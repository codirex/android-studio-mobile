package org.codirex.asm.domain.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.codirex.asm.domain.environment.EnvironmentException.Reason;
import org.junit.Test;

public final class EnvironmentVariablesTest {

  private static final class MemoryStore implements EnvironmentStore {
    List<EnvironmentVariable> content = new ArrayList<>();
    int saves;
    boolean failing;

    @Override
    public List<EnvironmentVariable> load() throws IOException {
      if (failing) {
        throw new IOException("disk");
      }
      return new ArrayList<>(content);
    }

    @Override
    public void save(List<EnvironmentVariable> variables) throws IOException {
      if (failing) {
        throw new IOException("disk");
      }
      saves++;
      content = new ArrayList<>(variables);
    }
  }

  private static void assertRejected(Reason reason, String name, String value) {
    try {
      new EnvironmentVariable(name, value);
      fail("accepted " + name);
    } catch (EnvironmentException e) {
      assertEquals(reason, e.reason());
    }
  }

  @Test
  public void aVariableIsAddedAndListedByName() throws Exception {
    EnvironmentVariables variables = new EnvironmentVariables(new MemoryStore());
    variables.set("ZED", "1");
    variables.set("  ALPHA ", "two words");
    List<EnvironmentVariable> list = variables.list();
    assertEquals(2, list.size());
    assertEquals("ALPHA", list.get(0).name());
    assertEquals("two words", list.get(0).value());
    assertEquals("ZED", list.get(1).name());
  }

  @Test
  public void settingAnExistingNameReplacesTheValue() throws Exception {
    EnvironmentVariables variables = new EnvironmentVariables(new MemoryStore());
    variables.set("GRADLE_OPTS", "-Xmx512m");
    variables.set("GRADLE_OPTS", "-Xmx1g");
    List<EnvironmentVariable> list = variables.list();
    assertEquals(1, list.size());
    assertEquals("-Xmx1g", list.get(0).value());
  }

  @Test
  public void aValueMayBeEmpty() throws Exception {
    EnvironmentVariables variables = new EnvironmentVariables(new MemoryStore());
    variables.set("EMPTY", "");
    assertEquals("", variables.list().get(0).value());
  }

  @Test
  public void removingDeletesOnlyThatName() throws Exception {
    MemoryStore store = new MemoryStore();
    EnvironmentVariables variables = new EnvironmentVariables(store);
    variables.set("A", "1");
    variables.set("B", "2");
    variables.remove("A");
    assertEquals(1, variables.list().size());
    assertEquals("B", variables.list().get(0).name());
  }

  @Test
  public void removingAMissingNameWritesNothing() throws Exception {
    MemoryStore store = new MemoryStore();
    EnvironmentVariables variables = new EnvironmentVariables(store);
    variables.set("A", "1");
    int before = store.saves;
    variables.remove("NOPE");
    assertEquals(before, store.saves);
  }

  @Test
  public void namesFollowTheShellRules() {
    assertTrue(EnvironmentVariable.isValidName("JAVA_OPTS"));
    assertTrue(EnvironmentVariable.isValidName("_x1"));
    assertFalse(EnvironmentVariable.isValidName(""));
    assertFalse(EnvironmentVariable.isValidName("1ABC"));
    assertFalse(EnvironmentVariable.isValidName("A-B"));
    assertFalse(EnvironmentVariable.isValidName("A B"));
    assertFalse(EnvironmentVariable.isValidName("A=B"));
    assertRejected(Reason.INVALID_NAME, "A=B", "x");
    assertRejected(Reason.INVALID_NAME, "", "x");
  }

  @Test
  public void theNamesTheTerminalNeedsAreReserved() {
    assertRejected(Reason.RESERVED_NAME, "HOME", "/");
    assertRejected(Reason.RESERVED_NAME, "PREFIX", "/");
    assertRejected(Reason.RESERVED_NAME, "SHELL", "/bin/sh");
    assertRejected(Reason.RESERVED_NAME, "LD_PRELOAD", "x");
    assertFalse(EnvironmentVariable.isReserved("PATH"));
    assertFalse(EnvironmentVariable.isReserved("JAVA_HOME"));
  }

  @Test
  public void valuesWithNulOrTooLongAreRefused() {
    assertRejected(Reason.INVALID_VALUE, "A", "a\0b");
    StringBuilder longValue = new StringBuilder();
    for (int i = 0; i <= EnvironmentVariable.MAX_VALUE_LENGTH; i++) {
      longValue.append('x');
    }
    assertRejected(Reason.INVALID_VALUE, "A", longValue.toString());
  }

  @Test
  public void aFailingStoreIsReportedAsStorage() throws Exception {
    MemoryStore store = new MemoryStore();
    store.failing = true;
    try {
      new EnvironmentVariables(store).set("A", "1");
      fail("no failure");
    } catch (EnvironmentException e) {
      assertEquals(Reason.STORAGE, e.reason());
    }
  }
}
