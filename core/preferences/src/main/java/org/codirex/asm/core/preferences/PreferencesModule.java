package org.codirex.asm.core.preferences;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/** Hilt wiring for {@code core:preferences}. Installed automatically in the application graph. */
@Module
@InstallIn(SingletonComponent.class)
abstract class PreferencesModule {

  @Binds
  abstract PreferenceStore bindPreferenceStore(SharedPreferencesStore impl);
}
