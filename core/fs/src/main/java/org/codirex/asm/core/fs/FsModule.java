package org.codirex.asm.core.fs;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/** Hilt wiring for {@code core:fs}. Installed automatically in the application graph. */
@Module
@InstallIn(SingletonComponent.class)
abstract class FsModule {

  @Binds
  abstract FileOperations bindFileOperations(LocalFileOperations impl);
}
