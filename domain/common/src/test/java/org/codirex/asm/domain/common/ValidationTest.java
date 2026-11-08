package org.codirex.asm.domain.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public final class ValidationTest {

  @Test
  public void okIsValid() {
    assertTrue(Validation.ok().isValid());
    assertNull(Validation.ok().error());
  }

  @Test
  public void errorCarriesItsCode() {
    Validation v = Validation.error("name.empty");
    assertFalse(v.isValid());
    assertEquals("name.empty", v.error());
    assertEquals(Validation.error("name.empty"), v);
  }

  @Test
  public void rejectsEmptyCode() {
    try {
      Validation.error("");
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // ok
    }
  }
}
