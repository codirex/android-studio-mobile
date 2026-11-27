package org.codirex.asm.feature.editor;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.codirex.asm.domain.xmlassist.AttrCatalog;

/**
 * The attributes of the Android framework, read once from the newest SDK platform that is installed
 * (files/sdk/platforms/android-N/data/res/values/attrs.xml, ADR-0030). There is none until the
 * person installs a platform in the SDK manager; the next completion then finds it.
 */
final class AndroidAttrs {

  private static final String TAG = "AndroidAttrs";
  private static AttrCatalog catalog;

  private AndroidAttrs() {}

  /** The catalog, or null if no platform is installed or its attrs.xml cannot be read. */
  static synchronized AttrCatalog get(File filesDirectory) {
    if (catalog != null) {
      return catalog;
    }
    File best = null;
    int bestApi = -1;
    File[] platforms = new File(filesDirectory, "sdk/platforms").listFiles();
    if (platforms != null) {
      for (File platform : platforms) {
        String name = platform.getName();
        if (!name.startsWith("android-")) {
          continue;
        }
        File attrs = new File(platform, "data/res/values/attrs.xml");
        int api;
        try {
          api = Integer.parseInt(name.substring("android-".length()));
        } catch (NumberFormatException e) {
          continue;
        }
        if (attrs.isFile() && api > bestApi) {
          best = attrs;
          bestApi = api;
        }
      }
    }
    if (best == null) {
      return null;
    }
    try (InputStream in = new FileInputStream(best)) {
      catalog = AttrCatalog.parse(in);
    } catch (IOException | RuntimeException e) {
      Log.w(TAG, "cannot read " + best, e);
    }
    return catalog;
  }
}
