package org.codirex.asm.domain.project;

import java.util.Objects;

/**
 * A project that exists. Its identity is its {@link ProjectName}: two projects never share a name.
 * Immutable.
 */
public final class Project {

  private final ProjectSpec spec;
  private final long createdAt;

  /**
   * @param spec what the project was created from
   * @param createdAt creation time in milliseconds since the epoch
   */
  public Project(ProjectSpec spec, long createdAt) {
    this.spec = Objects.requireNonNull(spec, "spec");
    this.createdAt = createdAt;
  }

  public ProjectSpec spec() {
    return spec;
  }

  public ProjectName name() {
    return spec.name();
  }

  public PackageName packageName() {
    return spec.packageName();
  }

  public ProjectType type() {
    return spec.type();
  }

  /** Creation time in milliseconds since the epoch. */
  public long createdAt() {
    return createdAt;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Project)) {
      return false;
    }
    Project that = (Project) other;
    return createdAt == that.createdAt && spec.equals(that.spec);
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec, createdAt);
  }

  @Override
  public String toString() {
    return "Project(" + spec + ", createdAt=" + createdAt + ")";
  }
}
