package org.codirex.asm.domain.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public final class ProjectSpecTest {

  private static ProjectSpec spec(ProjectType type) {
    return new ProjectSpec(ProjectName.of("Demo"), PackageName.of("org.codirex.demo"), type);
  }

  @Test
  public void equalityIsByValue() {
    assertEquals(spec(ProjectType.APPLICATION), spec(ProjectType.APPLICATION));
    assertNotEquals(spec(ProjectType.APPLICATION), spec(ProjectType.JAVA_LIBRARY));
  }

  @Test(expected = NullPointerException.class)
  public void rejectsNull() {
    new ProjectSpec(null, PackageName.of("org.codirex.demo"), ProjectType.APPLICATION);
  }
}
