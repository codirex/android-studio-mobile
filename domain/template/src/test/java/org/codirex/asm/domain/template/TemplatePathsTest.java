package org.codirex.asm.domain.template;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TemplatePathsTest {

  @Test
  public void ordinaryRelativePathsAreSafe() {
    assertTrue(TemplatePaths.isSafe("build.gradle.kts"));
    assertTrue(TemplatePaths.isSafe("app/src/main/java/org/codirex/demo/Main.java"));
    assertTrue(TemplatePaths.isSafe(".gitignore"));
    assertTrue(TemplatePaths.isSafe("src/.asm"));
    assertTrue(TemplatePaths.isSafe(".asmx/file"));
  }

  @Test
  public void emptyAbsoluteAndNullPathsAreNotSafe() {
    assertFalse(TemplatePaths.isSafe(null));
    assertFalse(TemplatePaths.isSafe(""));
    assertFalse(TemplatePaths.isSafe("/etc/passwd"));
  }

  @Test
  public void dotSegmentsAndEmptySegmentsAreNotSafe() {
    assertFalse(TemplatePaths.isSafe(".."));
    assertFalse(TemplatePaths.isSafe("../x"));
    assertFalse(TemplatePaths.isSafe("a/../x"));
    assertFalse(TemplatePaths.isSafe("a/./x"));
    assertFalse(TemplatePaths.isSafe("a//x"));
    assertFalse(TemplatePaths.isSafe("a/"));
  }

  @Test
  public void backslashesAndNulAreNotSafe() {
    assertFalse(TemplatePaths.isSafe("a\\..\\x"));
    assertFalse(TemplatePaths.isSafe("a\0b"));
  }

  @Test
  public void theInternalFolderIsNotSafe() {
    assertFalse(TemplatePaths.isSafe(".asm"));
    assertFalse(TemplatePaths.isSafe(".asm/project.properties"));
  }
}
