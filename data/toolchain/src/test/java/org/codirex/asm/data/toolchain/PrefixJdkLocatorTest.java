package org.codirex.asm.data.toolchain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.codirex.asm.domain.toolchain.InstalledJdk;
import org.codirex.asm.domain.toolchain.Toolchain;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class PrefixJdkLocatorTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private File jdkHome(File prefix) {
    return new File(prefix, Toolchain.JDK_17.homeFolder());
  }

  @Test
  public void aMissingJdkIsNotFound() throws Exception {
    assertNull(new PrefixJdkLocator(tmp.newFolder("usr")).find(Toolchain.JDK_17));
  }

  @Test
  public void aFolderWithoutTheJavaLauncherIsNotAJdk() throws Exception {
    File prefix = tmp.newFolder("usr");
    assertEquals(true, new File(jdkHome(prefix), "lib").mkdirs());
    assertNull(new PrefixJdkLocator(prefix).find(Toolchain.JDK_17));
  }

  @Test
  public void aJdkIsFoundWithItsVersion() throws Exception {
    File prefix = tmp.newFolder("usr");
    File home = jdkHome(prefix);
    assertEquals(true, new File(home, "bin").mkdirs());
    Files.write(new File(home, "bin/java").toPath(), new byte[] {1});
    Files.write(
        new File(home, "release").toPath(),
        "IMPLEMENTOR=\"Termux\"\nJAVA_VERSION=\"17.0.12\"\nOS_NAME=\"Linux\"\n"
            .getBytes(StandardCharsets.UTF_8));
    InstalledJdk found = new PrefixJdkLocator(prefix).find(Toolchain.JDK_17);
    assertEquals(home.getPath(), found.home());
    assertEquals("17.0.12", found.version());
  }

  @Test
  public void aJdkWithoutAReleaseFileHasAnUnknownVersion() throws Exception {
    File prefix = tmp.newFolder("usr");
    File home = jdkHome(prefix);
    assertEquals(true, new File(home, "bin").mkdirs());
    Files.write(new File(home, "bin/java").toPath(), new byte[] {1});
    assertEquals("", new PrefixJdkLocator(prefix).find(Toolchain.JDK_17).version());
  }
}
