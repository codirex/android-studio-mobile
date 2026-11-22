package org.codirex.asm.domain.toolchain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the output of a Gradle build line by line (run with {@code --console=plain}) and keeps what
 * a screen needs: the tasks, the compiler's errors and warnings with their places, and the result
 * (ADR-0032). It knows Gradle's own lines, javac's and kotlinc's; every other line is ignored.
 *
 * <p>Not thread safe: feed it from one thread and hand the {@link #report()} to others.
 */
public final class GradleOutputParser {

  private static final Pattern TASK = Pattern.compile("^> Task (\\S+)(?: ([A-Z][A-Z-]*))?\\s*$");
  private static final Pattern JAVAC =
      Pattern.compile("^(.+?\\.java):(\\d+): (error|warning): (.*)$");
  private static final Pattern KOTLIN_URI =
      Pattern.compile("^([ew]): (?:file://)?(.+?\\.kts?):(\\d+):(\\d+) (.*)$");
  private static final Pattern KOTLIN_PAREN =
      Pattern.compile("^([ew]): (.+?\\.kts?): \\((\\d+), (\\d+)\\): (.*)$");
  private static final Pattern RESULT =
      Pattern.compile("^BUILD (SUCCESSFUL|FAILED)(?: in (.+))?$");

  private final List<BuildReport.Task> tasks = new ArrayList<>();
  private final List<BuildProblem> problems = new ArrayList<>();
  private BuildReport.Status status = BuildReport.Status.RUNNING;
  private final StringBuilder failure = new StringBuilder();
  private boolean inFailure;
  private String duration = "";

  /** Reads one line, without its line break. */
  public void accept(String line) {
    if (line == null) {
      return;
    }
    if (inFailure) {
      if (line.startsWith("* ") || line.startsWith("BUILD ")) {
        inFailure = false;
      } else {
        if (!line.trim().isEmpty()) {
          if (failure.length() > 0) {
            failure.append('\n');
          }
          failure.append(line.trim());
        }
        return;
      }
    }
    if (line.startsWith("* What went wrong:")) {
      inFailure = true;
      failure.setLength(0);
      return;
    }
    Matcher task = TASK.matcher(line);
    if (task.matches()) {
      task(task.group(1), task.group(2) == null ? "" : task.group(2));
      return;
    }
    Matcher result = RESULT.matcher(line);
    if (result.matches()) {
      status =
          "SUCCESSFUL".equals(result.group(1))
              ? BuildReport.Status.SUCCESS
              : BuildReport.Status.FAILED;
      duration = result.group(2) == null ? "" : result.group(2).trim();
      return;
    }
    Matcher javac = JAVAC.matcher(line);
    if (javac.matches()) {
      problems.add(
          new BuildProblem(
              "error".equals(javac.group(3))
                  ? BuildProblem.Severity.ERROR
                  : BuildProblem.Severity.WARNING,
              javac.group(1),
              number(javac.group(2)),
              0,
              javac.group(4)));
      return;
    }
    Matcher kotlin = KOTLIN_URI.matcher(line);
    if (kotlin.matches()) {
      problems.add(kotlin(kotlin));
      return;
    }
    kotlin = KOTLIN_PAREN.matcher(line);
    if (kotlin.matches()) {
      problems.add(kotlin(kotlin));
    }
  }

  private static BuildProblem kotlin(Matcher m) {
    return new BuildProblem(
        "e".equals(m.group(1)) ? BuildProblem.Severity.ERROR : BuildProblem.Severity.WARNING,
        m.group(2),
        number(m.group(3)),
        number(m.group(4)),
        m.group(5));
  }

  private void task(String name, String outcome) {
    // "> Task :a" starts a task; the same name with an outcome on a later line ends it. With
    // --console=plain Gradle prints the outcome on the same line, so a task is usually seen once.
    for (int i = tasks.size() - 1; i >= 0; i--) {
      if (tasks.get(i).name().equals(name)) {
        tasks.set(i, new BuildReport.Task(name, outcome));
        return;
      }
    }
    tasks.add(new BuildReport.Task(name, outcome));
  }

  private static int number(String digits) {
    try {
      return Integer.parseInt(digits);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /** The state of the build as read so far. */
  public BuildReport report() {
    return new BuildReport(status, tasks, problems, failure.toString(), duration);
  }
}
