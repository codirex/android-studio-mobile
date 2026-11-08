package org.codirex.asm.core.concurrency;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;
import org.codirex.clotho.Realm;

/** Hilt wiring for {@code core:concurrency}. Installed automatically in the application graph. */
@Module
@InstallIn(SingletonComponent.class)
abstract class ConcurrencyModule {

  @Binds
  abstract RealmFactory bindRealmFactory(DefaultRealmFactory impl);

  /**
   * The process-wide realm. Never closed explicitly: it ends with the process, and jobs that must
   * not be lost are persisted by their owners.
   */
  @Provides
  @Singleton
  @ApplicationRealm
  static Realm provideApplicationRealm(RealmFactory factory) {
    return factory.open("application");
  }
}
