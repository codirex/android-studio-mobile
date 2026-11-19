package org.codirex.asm.domain.sdk;

import java.util.Objects;

/** The zip file of a package: where it is, how big and its SHA-1. Immutable. */
public final class SdkArchive {

  private final String url;
  private final String sha1;
  private final long size;

  /**
   * @param url the address relative to the repository, as the catalog states it
   * @param sha1 the SHA-1 of the file as 40 hexadecimal characters
   * @param size the size in bytes
   */
  public SdkArchive(String url, String sha1, long size) {
    this.url = Objects.requireNonNull(url, "url");
    this.sha1 = Objects.requireNonNull(sha1, "sha1").toLowerCase(java.util.Locale.ROOT);
    this.size = size;
  }

  public String url() {
    return url;
  }

  public String sha1() {
    return sha1;
  }

  public long size() {
    return size;
  }
}
