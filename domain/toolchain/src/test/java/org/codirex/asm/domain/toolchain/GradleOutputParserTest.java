package org.codirex.asm.domain.toolchain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GradleOutputParserTest {

  private static BuildReport parse(String... lines) {
    GradleOutputParser parser = new GradleOutputParser();
    for (String line : lines) {
      parser.accept(line);
    }
    return parser.report();
  }

  @Test
  public void aSuccessfulBuildHasItsTasksAndDuration() {
    BuildReport report =
        parse(
            "> Task :app:preBuild UP-TO-DATE",
            "> Task :app:compileDebugJavaWithJavac",
            "> Task :app:assembleDebug",
            "",
            "BUILD SUCCESSFUL in 1m 3s",
            "42 actionable tasks: 5 executed, 37 up-to-date");
    assertEquals(BuildReport.Status.SUCCESS, report.status());
    assertEquals("1m 3s", report.duration());
    assertEquals(3, report.tasks().size());
    assertEquals(new BuildReport.Task(":app:preBuild", "UP-TO-DATE"), report.tasks().get(0));
    assertEquals("", report.tasks().get(1).outcome());
  }

  @Test
  public void aBuildStillRunningHasNoResult() {
    BuildReport report = parse("> Task :app:preBuild");
    assertEquals(BuildReport.Status.RUNNING, report.status());
    assertEquals("", report.duration());
  }

  @Test
  public void aJavacErrorHasItsPlace() {
    BuildReport report =
        parse(
            "> Task :app:compileDebugJavaWithJavac FAILED",
            "/data/p/app/src/main/java/a/Main.java:12: error: cannot find symbol",
            "        foo();",
            "        ^",
            "  symbol:   method foo()");
    assertEquals(1, report.problems().size());
    BuildProblem problem = report.problems().get(0);
    assertEquals(BuildProblem.Severity.ERROR, problem.severity());
    assertEquals("/data/p/app/src/main/java/a/Main.java", problem.file());
    assertEquals(12, problem.line());
    assertEquals("cannot find symbol", problem.message());
    assertTrue(problem.hasLocation());
    assertEquals("FAILED", report.tasks().get(0).outcome());
  }

  @Test
  public void aJavacWarningIsAWarning() {
    BuildReport report = parse("/p/A.java:3: warning: [deprecation] x() in B has been deprecated");
    assertEquals(0, report.errorCount());
    assertEquals(1, report.warningCount());
  }

  @Test
  public void kotlinErrorsInBothFormats() {
    BuildReport report =
        parse(
            "e: file:///p/src/A.kt:7:13 Unresolved reference: x",
            "w: /p/src/B.kt: (4, 5): Variable 'y' is never used");
    assertEquals(2, report.problems().size());
    BuildProblem first = report.problems().get(0);
    assertEquals(BuildProblem.Severity.ERROR, first.severity());
    assertEquals("/p/src/A.kt", first.file());
    assertEquals(7, first.line());
    assertEquals(13, first.column());
    BuildProblem second = report.problems().get(1);
    assertEquals(BuildProblem.Severity.WARNING, second.severity());
    assertEquals(4, second.line());
    assertEquals(5, second.column());
  }

  @Test
  public void whatWentWrongIsCollectedUntilTheNextSection() {
    BuildReport report =
        parse(
            "FAILURE: Build failed with an exception.",
            "",
            "* What went wrong:",
            "Execution failed for task ':app:compileDebugJavaWithJavac'.",
            "> Compilation failed; see the compiler error output for details.",
            "",
            "* Try:",
            "> Run with --stacktrace option to get the stack trace.",
            "",
            "BUILD FAILED in 4s");
    assertEquals(BuildReport.Status.FAILED, report.status());
    assertEquals("4s", report.duration());
    assertEquals(
        "Execution failed for task ':app:compileDebugJavaWithJavac'.\n"
            + "> Compilation failed; see the compiler error output for details.",
        report.failure());
  }

  @Test
  public void otherLinesAreIgnored() {
    BuildReport report = parse("Starting a Gradle Daemon", "Downloading something", "");
    assertTrue(report.tasks().isEmpty());
    assertTrue(report.problems().isEmpty());
    assertEquals("", report.failure());
    assertFalse(report.status() == BuildReport.Status.FAILED);
  }

  @Test
  public void aTaskSeenAgainWithAnOutcomeIsUpdatedNotRepeated() {
    BuildReport report = parse("> Task :a", "> Task :a FAILED");
    assertEquals(1, report.tasks().size());
    assertEquals("FAILED", report.tasks().get(0).outcome());
  }
}
