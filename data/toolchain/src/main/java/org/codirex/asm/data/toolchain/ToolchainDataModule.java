package org.codirex.asm.data.toolchain;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import java.io.File;
import org.codirex.asm.domain.toolchain.JdkLocator;
import org.codirex.asm.domain.toolchain.Toolchain;

/** Hilt wiring for {@code data:toolchain}. Installed automatically in the application graph. */
@Module
@InstallIn(SingletonComponent.class)
abstract class ToolchainDataModule {

  /**
   * The userland prefix is {@code files/usr} (ADR-0022). The terminal module owns that rule; data
   * modules cannot depend on it (ADR-0003), so the folder name is repeated here.
   */
  @Provides
  static JdkLocator provideJdkLocator(@ApplicationContext Context context) {
    return new PrefixJdkLocator(new File(context.getFilesDir(), "usr"));
  }

  /** The domain classes have no Hilt annotations (ADR-0003), so they are built here. */
  @Provides
  static Toolchain provideToolchain(JdkLocator locator) {
    return new Toolchain(locator);
  }
}
