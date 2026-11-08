package org.codirex.asm;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

/** Entry point of the dependency graph. Keep it empty: work belongs in androidx.startup initializers. */
@HiltAndroidApp
public final class AsmApplication extends Application {}
