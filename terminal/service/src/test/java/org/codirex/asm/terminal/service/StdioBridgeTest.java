package org.codirex.asm.terminal.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.Assume;
import org.junit.Test;

public final class StdioBridgeTest {

  private static Process cat() throws Exception {
    try {
      return new ProcessBuilder("cat").start();
    } catch (java.io.IOException e) {
      Assume.assumeNoException(e);
      return null;
    }
  }

  @Test
  public void bytesGoToTheProgramAndBackAndTheEndOfTheProgramClosesTheConnection() throws Exception {
    Process process = cat();
    try (ServerSocket server = new ServerSocket(0);
        Socket client = new Socket("127.0.0.1", server.getLocalPort());
        Socket service = server.accept()) {
      StdioBridge.start(service.getInputStream(), service.getOutputStream(), service, process);
      OutputStream out = client.getOutputStream();
      InputStream in = client.getInputStream();
      byte[] message = "Content-Length: 2\r\n\r\néx".getBytes(StandardCharsets.UTF_8);
      out.write(message);
      out.flush();
      byte[] back = new byte[message.length];
      int done = 0;
      while (done < back.length) {
        int n = in.read(back, done, back.length - done);
        assertTrue(n > 0);
        done += n;
      }
      assertEquals(new String(message, StandardCharsets.UTF_8), new String(back, StandardCharsets.UTF_8));
      process.destroy();
      client.setSoTimeout(5000);
      assertEquals(-1, in.read());
    }
  }

  @Test
  public void aConnectionThatEndsStopsTheProgram() throws Exception {
    Process process = cat();
    try (ServerSocket server = new ServerSocket(0)) {
      Socket client = new Socket("127.0.0.1", server.getLocalPort());
      Socket service = server.accept();
      StdioBridge.start(service.getInputStream(), service.getOutputStream(), service, process);
      client.close();
      assertTrue(process.waitFor(5, TimeUnit.SECONDS));
    }
  }

  @Test
  public void workspaceKeysAreShortSafeNames() {
    assertTrue(LanguageServerLauncher.isValidKey("MyApp-1.0_x"));
    String[] bad = {"", ".", "..", "a/b", "a b", "../x", "x\u0000", null};
    for (String key : bad) {
      assertTrue(String.valueOf(key), !LanguageServerLauncher.isValidKey(key));
    }
  }
}
