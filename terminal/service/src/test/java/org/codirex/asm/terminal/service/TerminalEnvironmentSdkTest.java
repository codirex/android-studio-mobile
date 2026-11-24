package org.codirex.asm.terminal.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

  private static String read(File file) throws Exception {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }

  private static File installAapt2(File files) throws Exception {
    File aapt2 = new File(files, "usr/bin/aapt2");
    assertEquals(true, aapt2.getParentFile().mkdirs());
    Files.write(aapt2.toPath(), "x".getBytes(StandardCharsets.UTF_8));
    return aapt2;
  }

  @Test
  public void withoutTheUserlandAapt2NothingOverridesIt() throws Exception {
    File files = tmp.newFolder("f");
    Map<String, String> env = new TerminalEnvironment(files).variables();
    assertFalse(env.containsKey(AAPT2_OVERRIDE));
    assertFalse(new File(files, "home/.gradle/gradle.properties").exists());
  }

  @Test
  public void anInstalledAapt2IsHandedToGradleThroughTheUserHome() throws Exception {
    File files = tmp.newFolder("f");
    File aapt2 = installAapt2(files);
    Map<String, String> env = new TerminalEnvironment(files).variables();
    assertFalse(env.containsKey(AAPT2_OVERRIDE));
    String text = read(new File(files, "home/.gradle/gradle.properties"));
    assertTrue(text, text.contains("android.aapt2FromMavenOverride=" + aapt2.getPath() + "\n"));
  }

  @Test
  public void thePersonsOwnPropertiesSurviveAndTheBlockIsNotRepeated() throws Exception {
    File files = tmp.newFolder("f");
    File aapt2 = installAapt2(files);
    File properties = new File(files, "home/.gradle/gradle.properties");
    assertEquals(true, properties.getParentFile().mkdirs());
    Files.write(properties.toPath(), "org.gradle.daemon=false".getBytes(StandardCharsets.UTF_8));
    new TerminalEnvironment(files).variables();
    new TerminalEnvironment(files).variables();
    String text = read(properties);
    assertTrue(text, text.startsWith("org.gradle.daemon=false\n"));
    assertEquals(text, 1, text.split("android.aapt2FromMavenOverride", -1).length - 1);
    assertTrue(text, text.contains(aapt2.getPath()));
  }

  @Test
  public void theBlockGoesWhenAapt2IsRemoved() throws Exception {
    File files = tmp.newFolder("f");
    File aapt2 = installAapt2(files);
    File properties = new File(files, "home/.gradle/gradle.properties");
    new TerminalEnvironment(files).variables();
    assertEquals(true, properties.isFile());
    assertEquals(true, aapt2.delete());
    new TerminalEnvironment(files).variables();
    assertFalse(properties.exists());
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
