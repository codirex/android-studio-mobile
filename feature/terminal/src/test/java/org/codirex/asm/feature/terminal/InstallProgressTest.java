package org.codirex.asm.feature.terminal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InstallProgressTest {

  @Test
  public void percentIsProportional() {
    assertEquals(50, InstallProgress.percent(50, 100));
  }

  @Test
  public void percentIsClamped() {
    assertEquals(100, InstallProgress.percent(300, 100));
    assertEquals(0, InstallProgress.percent(-5, 100));
  }

  @Test
  public void percentIsUnknownWithoutTotal() {
    assertEquals(-1, InstallProgress.percent(10, 0));
    assertEquals(-1, InstallProgress.percent(10, -1));
  }

  @Test
  public void megabytesUseOneDecimal() {
    assertEquals("1.5", InstallProgress.megabytes(1536 * 1024));
    assertEquals("0.0", InstallProgress.megabytes(-1));
  }
}
