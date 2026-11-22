package org.codirex.asm.domain.toolchain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** What a Gradle build has said so far, in a form a screen can show. Immutable. */
public final class BuildReport {

  /** Where the build stands. */
  public enum Status {
    RUNNING,
    SUCCESS,
    FAILED
  }

  /** A task the build ran and how it went ({@code ""} while it runs, else Gradle's word). */
  public static final class Task {
    private final String name;
    private final String outcome;

    public Task(String name, String outcome) {
      this.name = Objects.requireNonNull(name, "name");
      this.outcome = Objects.requireNonNull(outcome, "outcome");
    }

    public String name() {
      return name;
    }

    /** {@code UP-TO-DATE}, {@code FROM-CACHE}, {@code NO-SOURCE}, {@code SKIPPED}, {@code FAILED}, or empty. */
    public String outcome() {
      return outcome;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Task
          && name.equals(((Task) other).name)
          && outcome.equals(((Task) other).outcome);
    }

    @Override
    public int hashCode() {
      return 31 * name.hashCode() + outcome.hashCode();
    }

    @Override
    public String toString() {
      return outcome.isEmpty() ? name : name + " " + outcome;
    }
  }

  private final Status status;
  private final List<Task> tasks;
  private final List<BuildProblem> problems;
  private final String failure;
  private final String duration;

  BuildReport(
      Status status,
      List<Task> tasks,
      List<BuildProblem> problems,
      String failure,
      String duration) {
    this.status = status;
    this.tasks = Collections.unmodifiableList(new ArrayList<>(tasks));
    this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
    this.failure = failure;
    this.duration = duration;
  }

  public Status status() {
    return status;
  }

  /** The tasks in the order they started. */
  public List<Task> tasks() {
    return tasks;
  }

  /** The compiler's errors and warnings, in the order they came. */
  public List<BuildProblem> problems() {
    return problems;
  }

  /** The "What went wrong" text of a failed build, or empty. */
  public String failure() {
    return failure;
  }

  /** The time Gradle reported ({@code 12s}, {@code 1m 3s}), or empty while it runs. */
  public String duration() {
    return duration;
  }

  public int errorCount() {
    int count = 0;
    for (BuildProblem problem : problems) {
      if (problem.severity() == BuildProblem.Severity.ERROR) {
        count++;
      }
    }
    return count;
  }

  public int warningCount() {
    return problems.size() - errorCount();
  }
}
