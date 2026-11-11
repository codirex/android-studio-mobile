package org.codirex.asm.terminal.bootstrap;

import android.system.ErrnoException;
import android.system.Os;
import java.io.File;
import java.io.IOException;

/** Creates symbolic links with {@code android.system.Os}, which every supported API has. */
public final class OsSymlinkCreator implements SymlinkCreator {

  @Override
  public void create(File link, String target) throws IOException {
    try {
      Os.symlink(target, link.getPath());
    } catch (ErrnoException e) {
      throw new IOException("cannot create the link " + link + " -> " + target, e);
    }
  }
}
