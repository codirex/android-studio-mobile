package org.codirex.asm.core.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public final class PrefTest {

  @Test
  public void keepsKeyAndDefault() {
    Pref<Boolean> pref = Pref.ofBoolean("editor.autosave", true);
    assertEquals("editor.autosave", pref.key());
    assertTrue(pref.defaultValue());
    assertEquals(Pref.Type.BOOLEAN, pref.type());
  }

  @Test
  public void typeFollowsTheFactory() {
    assertEquals(Pref.Type.INT, Pref.ofInt("a", 1).type());
    assertEquals(Pref.Type.STRING, Pref.ofString("b", "x").type());
  }

  @Test
  public void rejectsNullDefault() {
    try {
      Pref.ofString("b", null);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      // ok
    }
  }
}
