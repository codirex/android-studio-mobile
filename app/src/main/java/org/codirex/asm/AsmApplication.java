package org.codirex.asm;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

/**
 * Entry point of the dependency graph. Heavy work belongs in androidx.startup initializers, not
 * here. The look comes from Theme.Asm (the design palette, ADR-0034), so wallpaper colors are not
 * applied.
 */
@HiltAndroidApp
public final class AsmApplication extends Application {}
