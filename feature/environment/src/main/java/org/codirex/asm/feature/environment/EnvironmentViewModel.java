package org.codirex.asm.feature.environment;

import androidx.lifecycle.LiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import javax.inject.Inject;
import org.codirex.asm.core.concurrency.RealmFactory;
import org.codirex.asm.core.concurrency.RealmViewModel;
import org.codirex.asm.core.concurrency.UiStateHolder;
import org.codirex.asm.domain.environment.EnvironmentVariable;
import org.codirex.asm.domain.environment.EnvironmentVariables;

/**
 * Loads, sets and removes the environment variables off the main thread. All methods are called on
 * the main thread.
 */
@HiltViewModel
final class EnvironmentViewModel extends RealmViewModel {

  private final EnvironmentVariables variables;
  private final UiStateHolder<EnvironmentState> state =
      new UiStateHolder<>(EnvironmentState.initial());

  /** Bumped by every refresh; a result from an older refresh is ignored. */
  private int generation;

  @Inject
  EnvironmentViewModel(RealmFactory factory, EnvironmentVariables variables) {
    super(factory);
    this.variables = variables;
  }

  LiveData<EnvironmentState> state() {
    return state.observable();
  }

  /** Reads the variables again. */
  void refresh() {
    final int mine = ++generation;
    state.set(state.get().withLoading());
    this.<List<EnvironmentVariable>>runAsync(
        checkpoint -> variables.list(),
        outcome -> {
          if (mine != generation) {
            return;
          }
          if (outcome.isSuccess()) {
            state.set(state.get().loaded(outcome.valueOrNull()));
          } else if (outcome.isFailure()) {
            state.set(state.get().failed(EnvironmentFailure.of(outcome.failureOrNull())));
          }
        });
  }

  /** Adds the variable or changes its value, then reloads. */
  void save(final String name, final String value) {
    this.<Boolean>runAsync(
        checkpoint -> {
          variables.set(name, value);
          return Boolean.TRUE;
        },
        outcome -> {
          if (outcome.isFailure()) {
            state.set(state.get().failed(EnvironmentFailure.of(outcome.failureOrNull())));
          }
          refresh();
        });
  }

  /** Removes the variable, then reloads whatever happened. */
  void remove(final String name) {
    this.<Boolean>runAsync(
        checkpoint -> {
          variables.remove(name);
          return Boolean.TRUE;
        },
        outcome -> {
          if (outcome.isFailure()) {
            state.set(state.get().failed(EnvironmentFailure.of(outcome.failureOrNull())));
          }
          refresh();
        });
  }

  /** The screen showed the failure; do not show it again. */
  void failureHandled() {
    state.set(state.get().failureHandled());
  }
}
