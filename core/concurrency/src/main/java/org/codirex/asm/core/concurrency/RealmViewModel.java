package org.codirex.asm.core.concurrency;

import androidx.lifecycle.ViewModel;
import org.codirex.clotho.Realm;

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
