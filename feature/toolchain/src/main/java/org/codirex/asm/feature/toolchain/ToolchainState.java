package org.codirex.asm.feature.toolchain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codirex.asm.domain.toolchain.JdkStatus;

/** Everything the toolchain screen shows. Immutable. */
final class ToolchainState {

  private final List<JdkStatus> jdks;
  private final boolean loading;
  private final boolean failed;

  private ToolchainState(List<JdkStatus> jdks, boolean loading, boolean failed) {
    this.jdks = Collections.unmodifiableList(new ArrayList<>(jdks));
    this.loading = loading;
    this.failed = failed;
  }

  static ToolchainState initial() {
    return new ToolchainState(Collections.<JdkStatus>emptyList(), true, false);
  }

  ToolchainState withLoading() {
    return new ToolchainState(jdks, true, failed);
  }

  ToolchainState loaded(List<JdkStatus> newJdks) {
    return new ToolchainState(newJdks, false, failed);
  }

  ToolchainState failed() {
    return new ToolchainState(jdks, false, true);
  }

  ToolchainState failureHandled() {
    return new ToolchainState(jdks, loading, false);
  }

  List<JdkStatus> jdks() {
    return jdks;
  }

  boolean isLoading() {
    return loading;
  }

  /** The toolchain could not be read, and the screen has not told the person yet. */
  boolean hasFailed() {
    return failed;
  }
}
