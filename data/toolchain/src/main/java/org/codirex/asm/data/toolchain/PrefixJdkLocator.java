package org.codirex.asm.data.toolchain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.codirex.asm.domain.toolchain.InstalledJdk;
import org.codirex.asm.domain.toolchain.JdkLocator;
import org.codirex.asm.domain.toolchain.JdkOffer;

/**
 * Finds the JDKs below the userland prefix. A JDK counts as installed when its {@code bin/java}
 * exists; the version is read from the {@code release} file every JDK has.
 */
final class PrefixJdkLocator implements JdkLocator {

  private final File prefix;

  PrefixJdkLocator(File prefix) {
    this.prefix = prefix;
  }

  @Override
  public InstalledJdk find(JdkOffer offer) {
    File home = new File(prefix, offer.homeFolder());
    if (!new File(home, "bin/java").isFile()) {
      return null;
    }
    return new InstalledJdk(home.getPath(), versionOf(new File(home, "release")));
  }

  /** The {@code JAVA_VERSION} of a {@code release} file, or an empty text. */
  static String versionOf(File release) {
    if (!release.isFile()) {
      return "";
    }
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(release), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("JAVA_VERSION=")) {
          return line.substring("JAVA_VERSION=".length()).replace("\"", "").trim();
        }
      }
    } catch (IOException e) {
      // An unreadable release file only means the version is unknown.
    }
    return "";
  }
}
