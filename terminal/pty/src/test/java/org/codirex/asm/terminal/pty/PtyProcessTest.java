package org.codirex.asm.terminal.pty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * Runs only where the native library can be loaded (a JVM with {@code -Djava.library.path} set to
 * a build of {@code libasm_pty}); everywhere else the tests are skipped.
 */
public final class PtyProcessTest {

  private final Map<String, String> environment = new LinkedHashMap<>();

  @Before
  public void setUp() {
    assumeTrue(PtyProcess.isSupported());
    assumeTrue(new File("/bin/sh").canExecute());
    environment.put("TERM", "xterm-256color");
    environment.put("GREETING", "hello");
  }

  private static String readAll(PtyProcess process) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int read;
    while ((read = process.read(buffer, 0, buffer.length)) > 0) {
      out.write(buffer, 0, read);
    }
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }

  private PtyProcess sh(String script, int rows, int columns) throws IOException {
    return PtyProcess.start(
        "/bin/sh", Arrays.asList("sh", "-c", script), environment, null, rows, columns);
  }

  @Test
  public void outputAndExitCode() throws Exception {
    PtyProcess process = sh("echo $GREETING; exit 3", 24, 80);
    assertTrue(readAll(process).contains("hello"));
    assertEquals(3, process.waitFor());
    assertEquals(3, process.waitFor());
    process.close();
  }

  @Test
  public void theTerminalHasTheAskedSizeAndCanBeResized() throws Exception {
    PtyProcess process = sh("stty size; read x; stty size", 30, 100);
    process.resize(40, 120);
    process.write("go\n".getBytes(StandardCharsets.UTF_8), 0, 3);
    String text = readAll(process);
    assertEquals(0, process.waitFor());
    process.close();
    assertTrue(text, text.contains("40 120"));
  }

  @Test
  public void inputReachesTheProcessAndUtf8Survives() throws Exception {
    PtyProcess process = sh("read line; echo \"got:$line\"", 24, 80);
    byte[] input = "عربي\n".getBytes(StandardCharsets.UTF_8);
    process.write(input, 0, input.length);
    String text = readAll(process);
    process.waitFor();
    process.close();
    assertTrue(text, text.contains("got:عربي"));
  }

  @Test
  public void aMissingProgramIsAnIoException() {
    try {
      PtyProcess.start("/no/such/program", Collections.singletonList("x"), environment, null, 24,
          80);
      fail();
    } catch (IOException expected) {
      assertTrue(expected.getMessage(), expected.getMessage().contains("No such file"));
    }
  }

  @Test
  public void aBadWorkingDirectoryIsAnIoException() {
    try {
      PtyProcess.start("/bin/sh", Collections.singletonList("sh"), environment,
          new File("/no/such/dir"), 24, 80);
      fail();
    } catch (IOException expected) {
      // The child could not enter the directory.
    }
  }

  @Test
  public void closeEndsTheWholeProcessGroupAndTheReaderStops() throws Exception {
    final PtyProcess process = sh("sleep 100 & wait", 24, 80);
    Thread.sleep(200);
    final String[] seen = new String[1];
    Thread reader =
        new Thread(
            () -> {
              try {
                seen[0] = readAll(process);
              } catch (IOException e) {
                seen[0] = "error " + e;
              }
            });
    reader.start();
    process.close();
    reader.join(10_000);
    assertTrue("the reader is still blocked", !reader.isAlive());
    int code = process.waitFor();
    assertTrue("ended by a signal, got " + code, code == -PtyProcess.SIGHUP || code == -PtyProcess.SIGKILL);
    process.close();
  }

  @Test
  public void aClosedProcessReadsNothingAndRefusesWrites() throws Exception {
    PtyProcess process = sh("exit 0", 24, 80);
    process.waitFor();
    process.close();
    assertEquals(-1, process.read(new byte[8], 0, 8));
    try {
      process.write(new byte[1], 0, 1);
      fail();
    } catch (IOException expected) {
      // Closed.
    }
  }

  @Test
  public void interruptReachesTheForegroundProgram() throws Exception {
    PtyProcess process = sh("sleep 100; echo after", 24, 80);
    Thread.sleep(200);
    process.write(new byte[] {3}, 0, 1);
    readAll(process);
    process.waitFor();
    process.close();
  }

  @Test(expected = IllegalArgumentException.class)
  public void aZeroSizeIsRefused() throws Exception {
    PtyProcess.start("/bin/sh", Collections.singletonList("sh"), environment, null, 0, 80);
  }
}
