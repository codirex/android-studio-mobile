package org.codirex.asm.domain.toolchain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A Gradle task the person can run from the IDE, and the command line that runs it in the
 * terminal (ADR-0029). Immutable. The name is checked, so it can be put in a command line as it is.
 */
public final class GradleTask {

  private static final List<GradleTask> COMMON =
      Collections.unmodifiableList(
          Arrays.asList(
              new GradleTask("build"),
              new GradleTask("assembleDebug"),
              new GradleTask("test"),
              new GradleTask("clean"),
              new GradleTask("tasks")));

  private final String name;

  /**
   * @param name a task name or path such as {@code build} or {@code :app:assembleDebug}: letters,
   *     digits and {@code _ : . -}, not starting with a dash
   */
  public GradleTask(String name) {
    Objects.requireNonNull(name, "name");
    if (!isValidName(name)) {
      throw new IllegalArgumentException("not a task name: " + name);
    }
    this.name = name;
  }

  /** The tasks most projects have, in the order the screen offers them. */
  public static List<GradleTask> common() {
    return COMMON;
  }

  public static boolean isValidName(String name) {
    if (name.isEmpty() || name.charAt(0) == '-') {
      return false;
    }
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      boolean ok =
          (c >= 'a' && c <= 'z')
              || (c >= 'A' && c <= 'Z')
              || (c >= '0' && c <= '9')
              || c == '_'
              || c == ':'
              || c == '.'
              || c == '-';
      if (!ok) {
        return false;
      }
    }
    return true;
  }

  public String name() {
    return name;
  }

  /**
   * The command line that runs the task in the folder of the project: the project's own wrapper if
   * it has one (run through {@code sh}, so it needs no execute bit), else the {@code gradle} of the
   * userland.
   */
  public String command(boolean hasWrapper) {
    return (hasWrapper ? "sh ./gradlew " : "gradle ") + name;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof GradleTask && name.equals(((GradleTask) other).name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return "GradleTask(" + name + ")";
  }
}
