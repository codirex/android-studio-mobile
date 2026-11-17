package org.codirex.asm.domain.toolchain;

import java.util.Objects;

/** A JDK found on the device: where it is and which exact version it reports. Immutable. */
public final class InstalledJdk {

  private final String home;
  private final String version;

  /**
   * @param home the absolute path of the JDK, the value of {@code JAVA_HOME}
   * @param version the version the JDK reports, for example {@code 17.0.12}; empty if unknown
   */
  public InstalledJdk(String home, String version) {
    this.home = Objects.requireNonNull(home, "home");
    this.version = Objects.requireNonNull(version, "version");
  }

  public String home() {
    return home;
  }

  public String version() {
    return version;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof InstalledJdk)) {
      return false;
    }
    InstalledJdk that = (InstalledJdk) other;
    return home.equals(that.home) && version.equals(that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(home, version);
  }

  @Override
  public String toString() {
    return "InstalledJdk(" + home + ", " + version + ")";
  }
}
