package org.codirex.asm.feature.environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codirex.asm.domain.environment.EnvironmentVariable;

/** Everything the environment screen shows. Immutable. */
final class EnvironmentState {

  private final List<EnvironmentVariable> variables;
  private final boolean loading;
  private final boolean loadedOnce;
  private final EnvironmentFailure failure;

  private EnvironmentState(
      List<EnvironmentVariable> variables,
      boolean loading,
      boolean loadedOnce,
      EnvironmentFailure failure) {
    this.variables = Collections.unmodifiableList(new ArrayList<>(variables));
    this.loading = loading;
    this.loadedOnce = loadedOnce;
    this.failure = failure;
  }

  static EnvironmentState initial() {
    return new EnvironmentState(Collections.<EnvironmentVariable>emptyList(), true, false, null);
  }

  EnvironmentState withLoading() {
    return new EnvironmentState(variables, true, loadedOnce, failure);
  }

  EnvironmentState loaded(List<EnvironmentVariable> newVariables) {
    return new EnvironmentState(newVariables, false, true, failure);
  }

  EnvironmentState failed(EnvironmentFailure newFailure) {
    return new EnvironmentState(variables, false, loadedOnce, newFailure);
  }

  EnvironmentState failureHandled() {
    return new EnvironmentState(variables, loading, loadedOnce, null);
  }

  List<EnvironmentVariable> variables() {
    return variables;
  }

  boolean isLoading() {
    return loading;
  }

  /** The list is known and has nothing in it: the screen shows the empty text. */
  boolean isEmpty() {
    return loadedOnce && variables.isEmpty();
  }

  /** The failure the screen has not shown yet, or {@code null}. */
  EnvironmentFailure failure() {
    return failure;
  }
}
