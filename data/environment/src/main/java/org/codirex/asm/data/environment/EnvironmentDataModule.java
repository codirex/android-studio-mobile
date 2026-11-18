package org.codirex.asm.data.environment;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import java.io.File;
import org.codirex.asm.domain.environment.EnvironmentStore;
import org.codirex.asm.domain.environment.EnvironmentVariables;

/** Hilt wiring for {@code data:environment}. Installed automatically in the application graph. */
@Module
@InstallIn(SingletonComponent.class)
abstract class EnvironmentDataModule {

  /**
   * The file is {@code files/environment.properties}. The terminal service reads it from there; a
   * data module may not depend on {@code terminal:*} (ADR-0003), so the name is repeated in both.
   */
  @Provides
  static EnvironmentStore provideEnvironmentStore(@ApplicationContext Context context) {
    return new FileEnvironmentStore(new File(context.getFilesDir(), "environment.properties"));
  }

  /** The domain classes have no Hilt annotations (ADR-0003), so they are built here. */
  @Provides
  static EnvironmentVariables provideEnvironmentVariables(EnvironmentStore store) {
    return new EnvironmentVariables(store);
  }
}
