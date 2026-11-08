package org.codirex.asm.core.preferences;

import androidx.lifecycle.LiveData;

/**
 * Reads and writes user settings.
 *
 * <p>Inject this instead of touching {@code SharedPreferences}, so settings have one owner and
 * tests can replace the store.
 */
public interface PreferenceStore {

  /** The stored value, or the key's default if nothing is stored. */
  <T> T get(Pref<T> pref);

  /** Stores {@code value}. The change is visible to {@link #get} at once and written in the background. */
  <T> void set(Pref<T> pref, T value);

  /**
   * Observes a value. The observer gets the current value when it becomes active and again after
   * every change. Observe from the main thread.
   */
  <T> LiveData<T> observe(Pref<T> pref);
}
