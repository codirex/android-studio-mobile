package org.codirex.asm.terminal.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class TerminalEnvironmentJavaHomeTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private File jdk(File files, String name, boolean withJava) throws Exception {
    File home = new File(files, "usr/lib/jvm/" + name);
    assertTrue(new File(home, "bin").mkdirs());
    if (withJava) {
      Files.write(new File(home, "bin/java").toPath(), new byte[] {1});
    }
    return home;
  }

  @Test
  public void withoutAJdkThereIsNoJavaHome() throws Exception {
    File files = tmp.newFolder("f");
    TerminalEnvironment environment = new TerminalEnvironment(files);
    assertNull(environment.javaHome());
    assertFalse(environment.variables().containsKey("JAVA_HOME"));
  }

  @Test
  public void anInstalledJdkIsTheJavaHome() throws Exception {
    File files = tmp.newFolder("f");
    File home = jdk(files, "java-17-openjdk", true);
    assertEquals(home.getPath(), new TerminalEnvironment(files).variables().get("JAVA_HOME"));
  }

  @Test
  public void theNewestJdkWins() throws Exception {
    File files = tmp.newFolder("f");
    jdk(files, "java-17-openjdk", true);
    File newest = jdk(files, "java-21-openjdk", true);
    jdk(files, "java-8-openjdk", true);
    assertEquals(newest, new TerminalEnvironment(files).javaHome());
  }

  @Test
  public void aFolderWithoutTheLauncherIsNotAJdk() throws Exception {
    File files = tmp.newFolder("f");
    File real = jdk(files, "java-17-openjdk", true);
    jdk(files, "java-21-openjdk", false);
    assertEquals(real, new TerminalEnvironment(files).javaHome());
  }

  @Test
  public void folderNamesGiveTheirMajorVersion() {
    assertEquals(17, TerminalEnvironment.majorVersionOf("java-17-openjdk"));
    assertEquals(8, TerminalEnvironment.majorVersionOf("java-8"));
    assertEquals(-1, TerminalEnvironment.majorVersionOf("default-java"));
    assertEquals(-1, TerminalEnvironment.majorVersionOf("java-x-openjdk"));
  }
}
