package org.codirex.asm.feature.sdk;

import androidx.lifecycle.LiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import javax.inject.Inject;
import org.codirex.asm.core.concurrency.RealmFactory;
import org.codirex.asm.core.concurrency.RealmViewModel;
import org.codirex.asm.core.concurrency.UiStateHolder;
import org.codirex.asm.domain.sdk.SdkEntry;
import org.codirex.asm.domain.sdk.SdkManager;
import org.codirex.asm.domain.sdk.SdkPackage;
import org.codirex.asm.domain.sdk.TransferProgress;

/**
 * Lists, installs and removes SDK packages off the main thread. All methods are called on the main
 * thread.
 */
@HiltViewModel
final class SdkViewModel extends RealmViewModel {

  private final SdkManager manager;
  private final UiStateHolder<SdkState> state = new UiStateHolder<>(SdkState.initial());

  /** Percent of the running download, or -1 if it is not known. Posted by the download thread. */
  private final UiStateHolder<Integer> progress = new UiStateHolder<>(-1);

  private volatile boolean cancelled;

  /** Bumped by every refresh; a result from an older refresh is ignored. */
  private int generation;

  @Inject
  SdkViewModel(RealmFactory factory, SdkManager manager) {
    super(factory);
    this.manager = manager;
  }

  LiveData<SdkState> state() {
    return state.observable();
  }

  LiveData<Integer> progress() {
    return progress.observable();
  }

  /** Reads the catalog and what is installed. */
  void refresh() {
    final int mine = ++generation;
    state.set(state.get().withLoading());
    this.<List<SdkEntry>>runAsync(
        checkpoint -> manager.list(),
        outcome -> {
          if (mine != generation) {
            return;
          }
          if (outcome.isSuccess()) {
            state.set(state.get().loaded(outcome.valueOrNull()));
          } else if (outcome.isFailure()) {
            state.set(state.get().failed(SdkFailure.of(outcome.failureOrNull())));
          }
        });
  }

  /** Accepts the license (the person did, in the dialog) and installs or updates the package. */
  void install(final SdkPackage pkg) {
    if (state.get().isBusy()) {
      return;
    }
    cancelled = false;
    progress.set(-1);
    state.set(state.get().busy(pkg.displayName()));
    final TransferProgress transfer =
        new TransferProgress() {
          @Override
          public void onProgress(long done, long total) {
            progress.post(total > 0 ? (int) (done * 100 / total) : -1);
          }

          @Override
          public boolean isCancelled() {
            return cancelled;
          }
        };
    this.<Boolean>runAsync(
        checkpoint -> {
          manager.acceptLicense(pkg.license());
          manager.install(pkg, transfer);
          return Boolean.TRUE;
        },
        outcome -> {
          state.set(state.get().idle());
          if (outcome.isFailure()) {
            state.set(state.get().failed(SdkFailure.of(outcome.failureOrNull())));
          }
          refresh();
        });
  }

  /** Stops the running download. */
  void cancel() {
    cancelled = true;
  }

  /** Removes the package, then reloads whatever happened. */
  void remove(final SdkPackage pkg) {
    if (state.get().isBusy()) {
      return;
    }
    state.set(state.get().busy(pkg.displayName()));
    progress.set(-1);
    this.<Boolean>runAsync(
        checkpoint -> {
          manager.remove(pkg.path());
          return Boolean.TRUE;
        },
        outcome -> {
          state.set(state.get().idle());
          if (outcome.isFailure()) {
            state.set(state.get().failed(SdkFailure.of(outcome.failureOrNull())));
          }
          refresh();
        });
  }

  /** The screen showed the failure; do not show it again. */
  void failureHandled() {
    state.set(state.get().failureHandled());
  }
}
