package org.codirex.asm.terminal.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class TextScaleTest {

  @Test
  public void scalingMultiplies() {
    assertEquals(21f, TextScale.scaled(14f, 1.5f), 0.0001f);
  }

  @Test
  public void sizeStaysInRange() {
    assertEquals(TextScale.MAX_SP, TextScale.scaled(30f, 3f), 0f);
    assertEquals(TextScale.MIN_SP, TextScale.scaled(10f, 0.1f), 0f);
  }

  @Test
  public void badFactorsKeepTheSize() {
    assertEquals(14f, TextScale.scaled(14f, 0f), 0f);
    assertEquals(14f, TextScale.scaled(14f, -2f), 0f);
    assertEquals(14f, TextScale.scaled(14f, Float.NaN), 0f);
    assertEquals(14f, TextScale.scaled(14f, Float.POSITIVE_INFINITY), 0f);
  }

  @Test
  public void stepsAreHalves() {
    assertEquals(14f, TextScale.step(14.2f), 0f);
    assertEquals(14.5f, TextScale.step(14.3f), 0f);
    assertEquals(TextScale.MAX_SP, TextScale.step(99f), 0f);
  }
}
