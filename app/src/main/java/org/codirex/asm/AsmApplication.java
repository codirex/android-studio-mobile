package org.codirex.asm;

import android.app.Application;
import com.google.android.material.color.DynamicColors;
import dagger.hilt.android.HiltAndroidApp;

/** Entry point of the dependency graph. Heavy work belongs in androidx.startup initializers, not here. */
@HiltAndroidApp
public final class AsmApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    // Wallpaper colors on Android 12 and newer; a no-op on older versions.
    DynamicColors.applyToActivitiesIfAvailable(this);
  }
}
