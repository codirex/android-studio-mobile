package org.codirex.asm.terminal.bootstrap;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Where the bootstrap archives of one release are and which archive the app accepts: the base URL
 * of the release and the SHA-256 of the archive of each architecture, pinned in the app. An
 * architecture without a hash has no archive yet. Updating the userland means a new release and a
 * new hash in a new version of the app (ADR-0022).
 */
public final class BootstrapRelease {

  /** The release the app ships with. Hashes are of the archives attached to that release. */
  public static final BootstrapRelease DEFAULT =
      new BootstrapRelease(
          "https://github.com/codirex/asm-packages/releases/download/bootstrap-v0/",
          hashes(BootstrapArch.AARCH64,
              "1328ccf52e73552a5914a2b6107c2a957093bdf20dbebf8bb1187d8dcbb2ad5a"));

  private final String baseUrl;
  private final Map<BootstrapArch, String> sha256;

  /**
   * @param baseUrl the folder of the release, with a trailing slash
   * @param sha256 the lower case hex SHA-256 of the archive of each architecture that has one
   */
  public BootstrapRelease(String baseUrl, Map<BootstrapArch, String> sha256) {
    if (!Objects.requireNonNull(baseUrl, "baseUrl").endsWith("/")) {
      throw new IllegalArgumentException("baseUrl must end with a slash");
    }
    this.baseUrl = baseUrl;
    this.sha256 = Collections.unmodifiableMap(new EnumMap<>(sha256));
  }

  /** The URL of the archive of {@code arch}. */
  public String urlOf(BootstrapArch arch) {
    return baseUrl + arch.archiveName();
  }

  /** The expected SHA-256 of the archive of {@code arch}, or null if there is no archive yet. */
  public String sha256Of(BootstrapArch arch) {
    return sha256.get(arch);
  }

  private static Map<BootstrapArch, String> hashes(BootstrapArch arch, String sha256) {
    Map<BootstrapArch, String> map = new EnumMap<>(BootstrapArch.class);
    map.put(arch, sha256);
    return map;
  }
}
