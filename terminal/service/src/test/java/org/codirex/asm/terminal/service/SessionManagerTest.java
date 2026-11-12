package org.codirex.asm.terminal.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;

public final class SessionManagerTest {

  private final List<FakeProcess> started = new ArrayList<>();
  private final List<String> directories = new ArrayList<>();
  private final SessionManager manager =
      new SessionManager(
          (directory, rows, columns) -> {
            if ("bad".equals(directory)) {
              throw new IOException("cannot start");
            }
            FakeProcess process = new FakeProcess();
            process.rows = rows;
            process.columns = columns;
            directories.add(directory);
            started.add(process);
            return process;
          },
          1024);

  @After
  public void tearDown() {
    manager.closeAll();
  }

  @Test
  public void idsAreOrderedAndNeverReused() throws Exception {
    int a = manager.open("/a", 24, 80);
    int b = manager.open("/b", 24, 80);
    assertTrue(b > a);
    assertArrayEquals(new int[] {a, b}, manager.ids());
    manager.close(a);
    int c = manager.open("/c", 24, 80);
    assertTrue(c > b);
    assertArrayEquals(new int[] {b, c}, manager.ids());
  }

  @Test
  public void theProgramGetsTheDirectoryAndSize() throws Exception {
    manager.open("/work", 30, 100);
    assertEquals("/work", directories.get(0));
    assertEquals(30, started.get(0).rows);
    assertEquals(100, started.get(0).columns);
  }

  @Test
  public void aFailedStartLeavesNoSession() {
    try {
      manager.open("bad", 24, 80);
      fail();
    } catch (IOException expected) {
      assertEquals(0, manager.ids().length);
    }
  }

  @Test
  public void getFindsOpenSessionsOnly() throws Exception {
    int id = manager.open("/a", 24, 80);
    assertNotNull(manager.get(id));
    manager.close(id);
    assertNull(manager.get(id));
    assertTrue(started.get(0).closed);
  }

  @Test
  public void closingAnUnknownIdIsIgnored() {
    manager.close(99);
  }

  @Test
  public void closeAllEndsEverySession() throws Exception {
    manager.open("/a", 24, 80);
    manager.open("/b", 24, 80);
    manager.closeAll();
    assertEquals(0, manager.ids().length);
    assertTrue(started.get(0).closed);
    assertTrue(started.get(1).closed);
  }
}
