package org.codirex.asm.core.concurrency;

import androidx.lifecycle.ViewModel;
import org.codirex.clotho.Completion;
import org.codirex.clotho.Job;
import org.codirex.clotho.Lane;
import org.codirex.clotho.Realm;
import org.codirex.clotho.Work;

/**
 * Base class for view models that run background work.
 *
 * <p>Each instance owns one realm. It survives configuration changes with the view model and is
 * closed with {@link Realm#closeAsync()} when the view model is cleared: new work is rejected, running
 * jobs are asked to cancel, nothing blocks the main thread.
 *
 * <p>Subclasses are {@code @HiltViewModel} classes that take a {@link RealmFactory} and pass it up.
 */
public abstract class RealmViewModel extends ViewModel {

  private final Realm realm;

  protected RealmViewModel(RealmFactory factory) {
    this.realm = factory.open(getClass().getSimpleName());
  }

  /** The realm owned by this view model. */
  protected final Realm realm() {
    return realm;
  }

  /**
   * Runs {@code work} on the I/O lane, where blocking is allowed, and hands its outcome to {@code
   * onMain} on the main thread. The callback runs exactly once, also when the work fails or is
   * cancelled, and always after this method returned, never inside it.
   *
   * <p>Call it from the main thread. Cancel the returned job if the result is no longer wanted;
   * everything still running is cancelled when the view model is cleared.
   *
   * @param work what to run in the background
   * @param onMain what to do with the outcome on the main thread
   * @return the job, to cancel it
   */
  protected final <T> Job<T> runAsync(Work<T> work, Completion<? super T> onMain) {
    Job<T> job = realm.launch(Lane.io(), work);
    job.onComplete(Lane.main(), onMain);
    return job;
  }

  /** Called before the realm is closed. Release anything the view model holds besides jobs. */
  protected void onDispose() {}

  @Override
  protected final void onCleared() {
    try {
      onDispose();
    } finally {
      realm.closeAsync();
    }
  }
}
