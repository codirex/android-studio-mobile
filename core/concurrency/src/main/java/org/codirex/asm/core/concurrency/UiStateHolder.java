package org.codirex.asm.core.concurrency;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * The thin bridge between background work and the views: a value that any thread may publish and
 * the main thread observes.
 *
 * <p>It hides {@code LiveData} behind our own type so it can be replaced when Clotho gains a
 * reactive primitive, without touching the features that use it.
 *
 * @param <T> the state type; make it immutable
 */
public final class UiStateHolder<T> {

  private final MutableLiveData<T> live = new MutableLiveData<>();

  /** Creates a holder with no value yet. */
  public UiStateHolder() {}

  /** Creates a holder that starts with {@code initial}. */
  public UiStateHolder(T initial) {
    live.setValue(initial);
  }

  /** Publishes {@code state} from any thread. Values posted in quick succession may be merged. */
  public void post(T state) {
    live.postValue(state);
  }

  /** Publishes {@code state} immediately. Main thread only. */
  public void set(T state) {
    live.setValue(state);
  }

  /** The latest value, or {@code null} if none was published. */
  public T get() {
    return live.getValue();
  }

  /** Read-only stream for the views. */
  public LiveData<T> observable() {
    return live;
  }
}
