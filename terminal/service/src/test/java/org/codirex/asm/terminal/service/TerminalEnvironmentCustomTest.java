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

public final class TerminalEnvironmentCustomTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private File filesWith(String content) throws Exception {
    File files = tmp.newFolder("f");
    Files.write(new File(files, "environment.properties").toPath(), content.getBytes(StandardCharsets.UTF_8));
    return files;
  }

  @Test
  public void withoutAFileThereAreNoCustomVariables() throws Exception {
    assertTrue(new TerminalEnvironment(tmp.newFolder("f")).customVariables().isEmpty());
  }

  @Test
  public void theVariablesOfThePersonReachTheShell() throws Exception {
    File files = filesWith("GRADLE_OPTS=-Xmx1g\nGREETING=مرحبا\nWITH_EQUALS=a\\=b\n");
    Map<String, String> env = new TerminalEnvironment(files).variables();
    assertEquals("-Xmx1g", env.get("GRADLE_OPTS"));
    assertEquals("مرحبا", env.get("GREETING"));
    assertEquals("a=b", env.get("WITH_EQUALS"));
    assertEquals("xterm-256color", env.get("TERM"));
  }

  @Test
  public void aCustomVariableCanChangeADefaultButNotAReservedOne() throws Exception {
    File files = filesWith("LANG=ar_EG.UTF-8\nHOME=/evil\nPREFIX=/evil\nSHELL=/evil\nLD_PRELOAD=/evil\n");
    Map<String, String> env = new TerminalEnvironment(files).variables();
    assertEquals("ar_EG.UTF-8", env.get("LANG"));
    assertEquals(files + "/home", env.get("HOME"));
    assertEquals(files + "/usr", env.get("PREFIX"));
    assertEquals(files + "/usr/bin/bash", env.get("SHELL"));
    assertFalse(env.containsKey("LD_PRELOAD"));
  }

  @Test
  public void invalidNamesAreIgnored() throws Exception {
    File files = filesWith("OK=1\nBAD-NAME=2\n1ST=3\n");
    Map<String, String> custom = new TerminalEnvironment(files).customVariables();
    assertEquals(1, custom.size());
    assertEquals("1", custom.get("OK"));
  }

  @Test
  public void aCustomJavaHomeWinsOverTheDetectedOne() throws Exception {
    File files = filesWith("JAVA_HOME=/opt/my-jdk\n");
    File jdk = new File(files, "usr/lib/jvm/java-17-openjdk");
    assertTrue(new File(jdk, "bin").mkdirs());
    Files.write(new File(jdk, "bin/java").toPath(), new byte[] {1});
    assertEquals("/opt/my-jdk", new TerminalEnvironment(files).variables().get("JAVA_HOME"));
  }
}
