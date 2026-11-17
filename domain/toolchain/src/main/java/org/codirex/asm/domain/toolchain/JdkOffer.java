package org.codirex.asm.domain.toolchain;

import java.util.Objects;

/**
 * A JDK the IDE offers to install: which Java version it is, the package of the userland that
 * provides it and where that package puts it, relative to the userland prefix. Immutable.
 */
public final class JdkOffer {

  private final int majorVersion;
  private final String packageName;
  private final String homeFolder;

  /**
   * @param majorVersion the Java feature release, for example 17
   * @param packageName the package of the userland that installs it, for example {@code openjdk-17}
   * @param homeFolder where the JDK lives below the prefix, for example {@code
   *     lib/jvm/java-17-openjdk}
   */
  public JdkOffer(int majorVersion, String packageName, String homeFolder) {
    if (majorVersion < 1) {
      throw new IllegalArgumentException("majorVersion must be positive");
    }
    if (Objects.requireNonNull(packageName, "packageName").isEmpty()) {
      throw new IllegalArgumentException("packageName must not be empty");
    }
    if (Objects.requireNonNull(homeFolder, "homeFolder").isEmpty()
        || homeFolder.startsWith("/")
        || homeFolder.contains("..")) {
      throw new IllegalArgumentException("homeFolder must be a relative folder: " + homeFolder);
    }
    this.majorVersion = majorVersion;
    this.packageName = packageName;
    this.homeFolder = homeFolder;
  }

  public int majorVersion() {
    return majorVersion;
  }

  public String packageName() {
    return packageName;
  }

  public String homeFolder() {
    return homeFolder;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof JdkOffer)) {
      return false;
    }
    JdkOffer that = (JdkOffer) other;
    return majorVersion == that.majorVersion
        && packageName.equals(that.packageName)
        && homeFolder.equals(that.homeFolder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(majorVersion, packageName, homeFolder);
  }

  @Override
  public String toString() {
    return "JdkOffer(" + majorVersion + ", " + packageName + ")";
  }
}
