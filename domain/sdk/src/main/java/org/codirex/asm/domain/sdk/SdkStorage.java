package org.codirex.asm.domain.sdk;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Keeps the installed packages and the accepted licenses on the device. The domain defines it; a
 * data module implements it.
 *
 * <p><b>Every method blocks</b> and must be called from a background job.
 */
public interface SdkStorage {

  /** The installed packages: path to revision. */
  Map<String, String> installed() throws IOException;

  /**
   * Unpacks {@code archive} (a zip) as the package {@code path} and records {@code revision}. An
   * installed package of the same path is replaced. Nothing is left half installed on failure.
   */
  void install(String path, String revision, File archive) throws IOException;

  /** Removes the package; nothing happens if it is not installed. */
  void remove(String path) throws IOException;

  /** The hash of the text of the license the person accepted, or {@code null}. */
  String acceptedLicenseHash(String licenseId) throws IOException;

  /** Records that the person accepted the license with this hash. */
  void acceptLicense(String licenseId, String hash) throws IOException;
}
