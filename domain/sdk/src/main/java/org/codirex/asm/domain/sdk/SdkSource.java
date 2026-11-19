package org.codirex.asm.domain.sdk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Where the catalog and the archives come from (the repository of the SDK). The domain defines it;
 * a data module implements it.
 *
 * <p><b>Every method blocks</b> and must be called from a background job.
 */
public interface SdkSource {

  /** Opens the catalog document. The caller closes the stream. */
  InputStream openCatalog() throws IOException;

  /**
   * Downloads the archive into a new temporary file and returns it; the caller deletes it. Reports
   * the progress and stops with an {@link java.io.InterruptedIOException} if {@code progress}
   * says so.
   */
  File download(SdkArchive archive, TransferProgress progress) throws IOException;
}
