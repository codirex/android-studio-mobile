package org.codirex.asm.terminal.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class TerminalEnvironmentSdkTest {

  private static final String AAPT2_OVERRIDE = "ORG_GRADLE_PROJECT_android.aapt2FromMavenOverride";

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void theSdkFolderIsTheAndroidHome() throws Exception {
    File files = tmp.newFolder("f");
    Map<String, String> env = new TerminalEnvironment(files).variables();
    assertEquals(files + "/sdk", env.get("ANDROID_HOME"));
    assertEquals(files + "/sdk", env.get("ANDROID_SDK_ROOT"));
  }

  @Test
  public void withoutTheUserlandAapt2NothingOverridesIt() throws Exception {
    Map<String, String> env = new TerminalEnvironment(tmp.newFolder("f")).variables();
    assertFalse(env.containsKey(AAPT2_OVERRIDE));
  }

  @Test
  public void anInstalledAapt2IsHandedToGradle() throws Exception {
    File files = tmp.newFolder("f");
    File aapt2 = new File(files, "usr/bin/aapt2");
    assertEquals(true, aapt2.getParentFile().mkdirs());
    Files.write(aapt2.toPath(), "x".getBytes(StandardCharsets.UTF_8));
    Map<String, String> env = new TerminalEnvironment(files).variables();
    assertEquals(aapt2.getPath(), env.get(AAPT2_OVERRIDE));
  }

  @Test
  public void thePersonCanStillOverrideTheSdkLocation() throws Exception {
    File files = tmp.newFolder("f");
    Files.write(
        new File(files, "environment.properties").toPath(),
        "ANDROID_HOME=/elsewhere\n".getBytes(StandardCharsets.UTF_8));
    assertEquals("/elsewhere", new TerminalEnvironment(files).variables().get("ANDROID_HOME"));
  }
}
