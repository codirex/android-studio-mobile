package org.codirex.asm.feature.build;

import java.util.List;
import org.codirex.asm.domain.toolchain.BuildReport;

/** What the build screen shows: the parsed report, the raw tail of the output and whether it ended. */
final class BuildState {

  private final BuildReport report;
  private final List<String> tail;
  private final boolean finished;
  private final String error;

  BuildState(BuildReport report, List<String> tail, boolean finished, String error) {
    this.report = report;
    this.tail = tail;
    this.finished = finished;
    this.error = error;
  }

  BuildReport report() {
    return report;
  }

  /** The last lines of the raw output, for what the parser does not understand. */
  List<String> tail() {
    return tail;
  }

  /** Whether the output has ended (the build is over, was stopped, or could not start). */
  boolean isFinished() {
    return finished;
  }

  /** Why the build could not run, or empty. */
  String error() {
    return error;
  }
}
