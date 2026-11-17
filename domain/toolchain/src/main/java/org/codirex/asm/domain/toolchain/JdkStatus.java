package org.codirex.asm.domain.toolchain;

import java.util.Objects;

/** One offered JDK and whether it is installed. Immutable. */
public final class JdkStatus {

  private final JdkOffer offer;
  private final InstalledJdk installed;

  /** @param installed the JDK found on the device, or {@code null} */
  public JdkStatus(JdkOffer offer, InstalledJdk installed) {
    this.offer = Objects.requireNonNull(offer, "offer");
    this.installed = installed;
  }

  public JdkOffer offer() {
    return offer;
  }

  public boolean isInstalled() {
    return installed != null;
  }

  /** The installed JDK, or {@code null} if {@link #isInstalled()} is false. */
  public InstalledJdk installed() {
    return installed;
  }
}
