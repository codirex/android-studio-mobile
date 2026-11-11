package org.codirex.asm.terminal.bootstrap;

import java.io.File;
import java.io.IOException;

/**
 * Creates symbolic links. {@code java.nio.file.Files} can do it only from API 26 and the app
 * supports API 24, so the device implementation uses {@code android.system.Os}; tests use nio.
 */
public interface SymlinkCreator {

  /**
   * Creates {@code link} pointing to {@code target}. The target is stored as given, relative or
   * absolute, and need not exist. The parent folder of {@code link} exists.
   */
  void create(File link, String target) throws IOException;
}
