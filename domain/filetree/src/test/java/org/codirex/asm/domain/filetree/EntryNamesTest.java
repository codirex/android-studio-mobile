package org.codirex.asm.domain.filetree;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class EntryNamesTest {

  @Test
  public void ordinaryNamesAreValid() {
    assertTrue(EntryNames.isValid("Main.java"));
    assertTrue(EntryNames.isValid(".gitignore"));
    assertTrue(EntryNames.isValid("my file"));
    assertTrue(EntryNames.isValid("ملف.txt"));
    assertTrue(EntryNames.isValid("..."));
  }

  @Test
  public void emptyAndNullAreInvalid() {
    assertFalse(EntryNames.isValid(""));
    assertFalse(EntryNames.isValid(null));
  }

  @Test
  public void dotSegmentsAreInvalid() {
    assertFalse(EntryNames.isValid("."));
    assertFalse(EntryNames.isValid(".."));
  }

  @Test
  public void separatorsAndNulAreInvalid() {
    assertFalse(EntryNames.isValid("a/b"));
    assertFalse(EntryNames.isValid("/"));
    assertFalse(EntryNames.isValid("../x"));
    assertFalse(EntryNames.isValid("a\0b"));
  }

  @Test
  public void theLengthLimitIsInclusive() {
    StringBuilder ok = new StringBuilder();
    for (int i = 0; i < EntryNames.MAX_LENGTH; i++) {
      ok.append('a');
    }
    assertTrue(EntryNames.isValid(ok.toString()));
    assertFalse(EntryNames.isValid(ok + "a"));
  }
}
