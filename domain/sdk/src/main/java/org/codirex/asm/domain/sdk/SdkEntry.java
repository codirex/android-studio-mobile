package org.codirex.asm.domain.sdk;

import java.util.Objects;

/** A package of the catalog with its state on this device. Immutable. */
public final class SdkEntry {

  private final SdkPackage pkg;
  private final String installedRevision;
  private final boolean licenseAccepted;

  /**
   * @param installedRevision the revision installed on the device, or {@code null}
   * @param licenseAccepted whether the person accepted the current text of the license
   */
  public SdkEntry(SdkPackage pkg, String installedRevision, boolean licenseAccepted) {
    this.pkg = Objects.requireNonNull(pkg, "pkg");
    this.installedRevision = installedRevision;
    this.licenseAccepted = licenseAccepted;
  }

  public SdkPackage pkg() {
    return pkg;
  }

  public boolean isInstalled() {
    return installedRevision != null;
  }

  /** The installed revision, or {@code null}. */
  public String installedRevision() {
    return installedRevision;
  }

  /** Installed, but the catalog has a newer revision. */
  public boolean isUpdatable() {
    return installedRevision != null && !installedRevision.equals(pkg.revision());
  }

  public boolean isLicenseAccepted() {
    return licenseAccepted;
  }
}
