package org.codirex.asm.terminal.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class TerminalEnvironmentTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void thePathsFollowTheFilesDirectory() throws Exception {
    File files = tmp.newFolder("files");
    TerminalEnvironment environment = new TerminalEnvironment(files);
    Map<String, String> env = environment.variables();
    assertEquals(files + "/usr", env.get("PREFIX"));
    assertEquals(files + "/home", env.get("HOME"));
    assertEquals(files + "/usr/bin", env.get("PATH"));
    assertEquals(files + "/usr/tmp", env.get("TMPDIR"));
    assertEquals(files + "/usr/bin/bash", env.get("SHELL"));
    assertEquals(new File(files, "usr/bin/bash"), environment.shell());
    assertEquals(new File(files, "home"), environment.home());
  }

  @Test
  public void theTerminalIsDescribedAndNothingIsInherited() throws Exception {
    Map<String, String> env = new TerminalEnvironment(tmp.newFolder("f")).variables();
    assertEquals("xterm-256color", env.get("TERM"));
    assertEquals("truecolor", env.get("COLORTERM"));
    assertEquals("en_US.UTF-8", env.get("LANG"));
    assertEquals("/system", env.get("ANDROID_ROOT"));
    assertEquals("/data", env.get("ANDROID_DATA"));
    assertFalse(env.containsKey("LD_PRELOAD"));
    assertFalse(env.containsKey("CLASSPATH"));
  }

  @Test
  public void theShellIsALoginShell() throws Exception {
    assertEquals("-bash", new TerminalEnvironment(tmp.newFolder("f")).arguments().get(0));
  }

  @Test
  public void theNewestTermuxExecLibraryIsPreloaded() throws Exception {
    File files = tmp.newFolder("f");
    File lib = new File(files, "usr/lib");
    assertTrue(lib.mkdirs());
    Files.write(new File(lib, "libtermux-exec.so").toPath(), new byte[] {1});
    assertEquals(
        new File(lib, "libtermux-exec.so").getPath(),
        new TerminalEnvironment(files).variables().get("LD_PRELOAD"));
    Files.write(new File(lib, "libtermux-exec-ld-preload.so").toPath(), new byte[] {1});
    assertEquals(
        new File(lib, "libtermux-exec-ld-preload.so").getPath(),
        new TerminalEnvironment(files).variables().get("LD_PRELOAD"));
  }

  @Test
  public void aSessionStartsInTheAskedDirectoryOrHome() throws Exception {
    File files = tmp.newFolder("f");
    File project = tmp.newFolder("project");
    TerminalEnvironment environment = new TerminalEnvironment(files);
    assertEquals(project, environment.startDirectory(project.getPath()));
    assertEquals(environment.home(), environment.startDirectory(""));
    assertEquals(environment.home(), environment.startDirectory(null));
    assertEquals(environment.home(), environment.startDirectory("/no/such/folder"));
  }
}
