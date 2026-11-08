package org.codirex.asm.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

/** The production store: the app's default shared preferences file, which the settings screens use too. */
@Singleton
final class SharedPreferencesStore implements PreferenceStore {

  private final SharedPreferences prefs;

  @Inject
  SharedPreferencesStore(@ApplicationContext Context context) {
    this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(Pref<T> pref) {
    switch (pref.type()) {
      case BOOLEAN:
        return (T) Boolean.valueOf(prefs.getBoolean(pref.key(), (Boolean) pref.defaultValue()));
      case INT:
        return (T) Integer.valueOf(prefs.getInt(pref.key(), (Integer) pref.defaultValue()));
      default:
        return (T) prefs.getString(pref.key(), (String) pref.defaultValue());
    }
  }

  @Override
  public <T> void set(Pref<T> pref, T value) {
    SharedPreferences.Editor editor = prefs.edit();
    switch (pref.type()) {
      case BOOLEAN:
        editor.putBoolean(pref.key(), (Boolean) value);
        break;
      case INT:
        editor.putInt(pref.key(), (Integer) value);
        break;
      default:
        editor.putString(pref.key(), (String) value);
        break;
    }
    editor.apply();
  }

  @Override
  public <T> LiveData<T> observe(Pref<T> pref) {
    return new PrefLiveData<>(this, prefs, pref);
  }

  /** Listens to the file only while someone observes, so nothing leaks. */
  private static final class PrefLiveData<T> extends LiveData<T>
      implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final SharedPreferencesStore store;
    private final SharedPreferences prefs;
    private final Pref<T> pref;

    PrefLiveData(SharedPreferencesStore store, SharedPreferences prefs, Pref<T> pref) {
      this.store = store;
      this.prefs = prefs;
      this.pref = pref;
    }

    @Override
    protected void onActive() {
      setValue(store.get(pref));
      prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onInactive() {
      prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences changed, String key) {
      // A null key means the whole file was cleared.
      if (key == null || key.equals(pref.key())) {
        setValue(store.get(pref));
      }
    }
  }
}
