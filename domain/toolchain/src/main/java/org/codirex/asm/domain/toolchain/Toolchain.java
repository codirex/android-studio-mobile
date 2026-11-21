package org.codirex.asm.domain.toolchain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The use cases of the toolchain: which JDKs the IDE offers and which of them are installed
 * (ADR-0027). The JDK comes from the userland's package repository, so installing one is running
 * the package manager; this class only tells what is there.
 *
 * <p><b>Every method blocks.</b> Callers run it in a background job.
 */
public final class Toolchain {

  /** JDK 17: the one Gradle builds run on (ADR-0027). */
  public static final JdkOffer JDK_17 = new JdkOffer(17, "openjdk-17", "lib/jvm/java-17-openjdk");

  /** JDK 21: for projects that need it. */
  public static final JdkOffer JDK_21 = new JdkOffer(21, "openjdk-21", "lib/jvm/java-21-openjdk");

  /** JDK 25: the Java language server (jdt.ls 1.63.0) needs it to run (ADR-0031). */
  public static final JdkOffer JDK_25 = new JdkOffer(25, "openjdk-25", "lib/jvm/java-25-openjdk");

  private static final List<JdkOffer> OFFERS =
      Collections.unmodifiableList(Arrays.asList(JDK_25, JDK_21, JDK_17));

  private final JdkLocator locator;

  public Toolchain(JdkLocator locator) {
    this.locator = Objects.requireNonNull(locator, "locator");
  }

  /** The JDKs the IDE offers, newest first. */
  public static List<JdkOffer> offers() {
    return OFFERS;
  }

  /** Every offered JDK with its state. */
  public List<JdkStatus> jdks() {
    List<JdkStatus> result = new ArrayList<>(OFFERS.size());
    for (JdkOffer offer : OFFERS) {
      result.add(new JdkStatus(offer, locator.find(offer)));
    }
    return result;
  }
}
