package org.codirex.asm.domain.project;

/** The kinds of project the IDE can create. */
public enum ProjectType {
  APPLICATION("application"),
  ANDROID_LIBRARY("android-library"),
  JAVA_LIBRARY("java-library"),
  GRADLE_PLUGIN("gradle-plugin");

  private final String id;

  ProjectType(String id) {
    this.id = id;
  }

  /** A stable identifier safe to store in templates and files. Never changes between releases. */
  public String id() {
    return id;
  }

  /**
   * Finds a type by its {@link #id()}.
   *
   * @throws IllegalArgumentException if no type has this id
   */
  public static ProjectType fromId(String id) {
    for (ProjectType type : values()) {
      if (type.id.equals(id)) {
        return type;
      }
    }
    throw new IllegalArgumentException("unknown project type: " + id);
  }
}
