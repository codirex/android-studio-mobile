package org.codirex.asm.data.sdk;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import java.io.File;
import org.codirex.asm.domain.sdk.SdkManager;
import org.codirex.asm.domain.sdk.SdkSource;
import org.codirex.asm.domain.sdk.SdkStorage;

/** Hilt wiring for {@code data:sdk}. Installed automatically in the application graph. */
@Module
@InstallIn(SingletonComponent.class)
abstract class SdkDataModule {

  /** The SDK lives in {@code files/sdk}; the terminal sets {@code ANDROID_HOME} to it (ADR-0030). */
  @Provides
  static SdkStorage provideSdkStorage(@ApplicationContext Context context) {
    return new FileSdkStorage(new File(context.getFilesDir(), "sdk"));
  }

  @Provides
  static SdkSource provideSdkSource(@ApplicationContext Context context) {
    return new HttpSdkSource(HttpSdkSource.REPOSITORY, new File(context.getCacheDir(), "sdk"));
  }

  /** The domain classes have no Hilt annotations (ADR-0003), so they are built here. */
  @Provides
  static SdkManager provideSdkManager(SdkSource source, SdkStorage storage) {
    return new SdkManager(source, storage);
  }
}
