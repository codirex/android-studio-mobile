package org.codirex.asm.feature.toolchain;

import androidx.lifecycle.LiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import javax.inject.Inject;
import org.codirex.asm.core.concurrency.RealmFactory;
import org.codirex.asm.core.concurrency.RealmViewModel;
import org.codirex.asm.core.concurrency.UiStateHolder;
import org.codirex.asm.domain.toolchain.JdkStatus;
import org.codirex.asm.domain.toolchain.Toolchain;

/** Reads the state of the toolchain off the main thread. All methods are called on the main thread. */
@HiltViewModel
final class ToolchainViewModel extends RealmViewModel {

  private final Toolchain toolchain;
  private final UiStateHolder<ToolchainState> state =
      new UiStateHolder<>(ToolchainState.initial());

  /** Bumped by every refresh; a result from an older refresh is ignored. */
  private int generation;

  @Inject
  ToolchainViewModel(RealmFactory factory, Toolchain toolchain) {
    super(factory);
    this.toolchain = toolchain;
  }

  LiveData<ToolchainState> state() {
    return state.observable();
  }

  /** Reads the state again. */
  void refresh() {
    final int mine = ++generation;
    state.set(state.get().withLoading());
    this.<List<JdkStatus>>runAsync(
        checkpoint -> toolchain.jdks(),
        outcome -> {
          if (mine != generation) {
            return;
          }
          if (outcome.isSuccess()) {
            state.set(state.get().loaded(outcome.valueOrNull()));
          } else if (outcome.isFailure()) {
            state.set(state.get().failed());
          }
        });
  }

  /** The screen showed the failure; do not show it again. */
  void failureHandled() {
    state.set(state.get().failureHandled());
  }
}
