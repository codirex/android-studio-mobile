package org.codirex.asm.domain.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public final class ProjectTest {

  private static ProjectSpec spec() {
    return new ProjectSpec(
        ProjectName.of("Demo"), PackageName.of("org.codirex.demo"), ProjectType.APPLICATION);
  }

  @Test
  public void exposesTheSpecParts() {
    Project project = new Project(spec(), 42L);
    assertEquals(ProjectName.of("Demo"), project.name());
    assertEquals(PackageName.of("org.codirex.demo"), project.packageName());
    assertEquals(ProjectType.APPLICATION, project.type());
    assertEquals(42L, project.createdAt());
  }

  @Test
  public void equalityIncludesCreationTime() {
    assertEquals(new Project(spec(), 1L), new Project(spec(), 1L));
    assertNotEquals(new Project(spec(), 1L), new Project(spec(), 2L));
  }

  @Test
  public void exceptionKeepsItsReason() {
    ProjectException e = new ProjectException(ProjectException.Reason.NOT_FOUND, "x");
    assertEquals(ProjectException.Reason.NOT_FOUND, e.reason());
  }
}
