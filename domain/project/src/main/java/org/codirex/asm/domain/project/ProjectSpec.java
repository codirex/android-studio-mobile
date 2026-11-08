package org.codirex.asm.domain.project;

import java.util.Objects;

/** What the user asked for when creating a project. Immutable. */
public final class ProjectSpec {

  private final ProjectName name;
  private final PackageName packageName;
  private final ProjectType type;

  public ProjectSpec(ProjectName name, PackageName packageName, ProjectType type) {
    this.name = Objects.requireNonNull(name, "name");
    this.packageName = Objects.requireNonNull(packageName, "packageName");
    this.type = Objects.requireNonNull(type, "type");
  }

  public ProjectName name() {
    return name;
  }

  public PackageName packageName() {
    return packageName;
  }

  public ProjectType type() {
    return type;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ProjectSpec)) {
      return false;
    }
    ProjectSpec that = (ProjectSpec) other;
    return name.equals(that.name) && packageName.equals(that.packageName) && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, packageName, type);
  }

  @Override
  public String toString() {
    return "ProjectSpec(" + name + ", " + packageName + ", " + type.id() + ")";
  }
}
