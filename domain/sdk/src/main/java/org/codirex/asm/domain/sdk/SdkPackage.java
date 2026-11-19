package org.codirex.asm.domain.sdk;

import java.util.Objects;

/** A package of the SDK catalog: one thing that can be installed. Immutable. */
public final class SdkPackage {

  private final String path;
  private final String displayName;
  private final String revision;
  private final SdkLicense license;
  private final SdkArchive archive;

  /**
   * @param path the catalog path, for example {@code platforms;android-34}
   * @param revision the revision, for example {@code 3} or {@code 34.0.0}
   */
  public SdkPackage(
      String path, String displayName, String revision, SdkLicense license, SdkArchive archive) {
    this.path = Objects.requireNonNull(path, "path");
    this.displayName = Objects.requireNonNull(displayName, "displayName");
    this.revision = Objects.requireNonNull(revision, "revision");
    this.license = Objects.requireNonNull(license, "license");
    this.archive = Objects.requireNonNull(archive, "archive");
  }

  public String path() {
    return path;
  }

  public String displayName() {
    return displayName;
  }

  public String revision() {
    return revision;
  }

  public SdkLicense license() {
    return license;
  }

  public SdkArchive archive() {
    return archive;
  }
}
