package org.codirex.asm.domain.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public final class PackageNameTest {

  private static void assertError(String expected, String candidate) {
    assertEquals(expected, PackageName.validate(candidate).error());
  }

  @Test
  public void acceptsNormalNames() {
    assertTrue(PackageName.validate("org.codirex.asm").isValid());
    assertTrue(PackageName.validate("com.example2.my_app").isValid());
    assertTrue(PackageName.validate("a.b").isValid());
  }

  @Test
  public void rejectsEmpty() {
    assertError(PackageName.ERROR_EMPTY, null);
    assertError(PackageName.ERROR_EMPTY, "");
  }

  @Test
  public void needsTwoSegments() {
    assertError(PackageName.ERROR_TOO_FEW_SEGMENTS, "app");
  }

  @Test
  public void segmentsMustStartWithALetter() {
    assertError(PackageName.ERROR_SEGMENT_START, "org.1abc");
    assertError(PackageName.ERROR_SEGMENT_START, "org._abc");
    assertError(PackageName.ERROR_SEGMENT_START, "org.");
    assertError(PackageName.ERROR_SEGMENT_START, ".org");
    assertError(PackageName.ERROR_SEGMENT_START, "org..app");
  }

  @Test
  public void rejectsOtherCharacters() {
    assertError(PackageName.ERROR_CHARACTER, "org.my-app");
    assertError(PackageName.ERROR_CHARACTER, "org.my app");
    assertError(PackageName.ERROR_CHARACTER, "org.étude");
  }

  @Test
  public void rejectsJavaKeywords() {
    assertError(PackageName.ERROR_KEYWORD, "org.int");
    assertError(PackageName.ERROR_KEYWORD, "new.app");
    assertError(PackageName.ERROR_KEYWORD, "org.null");
  }

  @Test
  public void ofThrowsWithTheErrorCode() {
    try {
      PackageName.of("app");
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      assertEquals(PackageName.ERROR_TOO_FEW_SEGMENTS, expected.getMessage());
    }
  }

  @Test
  public void directoryPathUsesSlashes() {
    assertEquals("org/codirex/asm", PackageName.of("org.codirex.asm").toDirectoryPath());
  }

  @Test
  public void equalityIsByValue() {
    assertEquals(PackageName.of("org.codirex.asm"), PackageName.of("org.codirex.asm"));
  }
}
