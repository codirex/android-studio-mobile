package org.codirex.asm.domain.toolchain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public final class ToolchainTest {

  @Test
  public void aJdkThatIsNotInstalledIsReportedAsSuch() {
    Toolchain toolchain = new Toolchain(offer -> null);
    List<JdkStatus> jdks = toolchain.jdks();
    assertEquals(1, jdks.size());
    assertEquals(Toolchain.JDK_17, jdks.get(0).offer());
    assertFalse(jdks.get(0).isInstalled());
    assertNull(jdks.get(0).installed());
  }

  @Test
  public void anInstalledJdkCarriesWhatTheLocatorFound() {
    final InstalledJdk found = new InstalledJdk("/data/usr/lib/jvm/java-17-openjdk", "17.0.12");
    Toolchain toolchain = new Toolchain(offer -> found);
    JdkStatus status = toolchain.jdks().get(0);
    assertTrue(status.isInstalled());
    assertEquals("17.0.12", status.installed().version());
    assertEquals("/data/usr/lib/jvm/java-17-openjdk", status.installed().home());
  }

  @Test
  public void theOffersAreNewestFirstAndFixed() {
    List<JdkOffer> offers = Toolchain.offers();
    for (int i = 1; i < offers.size(); i++) {
      assertTrue(offers.get(i - 1).majorVersion() > offers.get(i).majorVersion());
    }
    assertEquals("openjdk-17", Toolchain.JDK_17.packageName());
  }

  @Test(expected = IllegalArgumentException.class)
  public void anAbsoluteHomeFolderIsRefused() {
    new JdkOffer(17, "openjdk-17", "/lib/jvm");
  }

  @Test(expected = IllegalArgumentException.class)
  public void aHomeFolderThatLeavesThePrefixIsRefused() {
    new JdkOffer(17, "openjdk-17", "../jvm");
  }
}
