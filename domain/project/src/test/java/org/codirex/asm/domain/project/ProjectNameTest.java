package org.codirex.asm.domain.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public final class ProjectNameTest {

  private static void assertError(String expected, String candidate) {
    assertEquals(expected, ProjectName.validate(candidate).error());
  }

  @Test
  public void acceptsNormalNames() {
    assertTrue(ProjectName.validate("My App").isValid());
    assertTrue(ProjectName.validate("demo-1_2").isValid());
    assertTrue(ProjectName.validate("تطبيقي").isValid());
  }

  @Test
  public void rejectsEmptyAndBlank() {
    assertError(ProjectName.ERROR_EMPTY, null);
    assertError(ProjectName.ERROR_EMPTY, "");
    assertError(ProjectName.ERROR_EMPTY, "   ");
  }

  @Test
  public void rejectsTooLong() {
    StringBuilder name = new StringBuilder();
    for (int i = 0; i <= ProjectName.MAX_LENGTH; i++) {
      name.append('a');
    }
    assertError(ProjectName.ERROR_TOO_LONG, name.toString());
    assertTrue(ProjectName.validate(name.substring(1)).isValid());
  }

  @Test
  public void rejectsReservedNames() {
    assertError(ProjectName.ERROR_RESERVED, ".");
    assertError(ProjectName.ERROR_RESERVED, "..");
  }

  @Test
  public void rejectsPathAndReservedCharacters() {
    assertError(ProjectName.ERROR_CHARACTER, "a/b");
    assertError(ProjectName.ERROR_CHARACTER, "a\\b");
    assertError(ProjectName.ERROR_CHARACTER, "a:b");
    assertError(ProjectName.ERROR_CHARACTER, "a*b");
    assertError(ProjectName.ERROR_CHARACTER, "a\u0000b");
    assertError(ProjectName.ERROR_CHARACTER, "a\nb");
  }

  @Test
  public void rejectsEdgeSpaceAndTrailingDot() {
    assertError(ProjectName.ERROR_CHARACTER, " app");
    assertError(ProjectName.ERROR_CHARACTER, "app ");
    assertError(ProjectName.ERROR_CHARACTER, "app.");
  }

  @Test
  public void ofThrowsWithTheErrorCode() {
    try {
      ProjectName.of("");
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      assertEquals(ProjectName.ERROR_EMPTY, expected.getMessage());
    }
  }
}
