package org.codirex.asm.data.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import org.codirex.asm.domain.sdk.SdkArchive;
import org.codirex.asm.domain.sdk.TransferProgress;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Runs against a minimal HTTP server made of a plain socket: com.sun.net.httpserver is not part of
 * the Android unit test class path.
 */
public final class HttpSdkSourceTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private ServerSocket server;
  private Thread thread;
  private byte[] body;
  private volatile int status = 200;

  @Before
  public void startServer() throws Exception {
    body = new byte[300_000];
    Arrays.fill(body, (byte) 7);
    server = new ServerSocket(0);
    thread =
        new Thread(
            () -> {
              while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                  InputStream in = socket.getInputStream();
                  ByteArrayOutputStream request = new ByteArrayOutputStream();
                  int last4 = 0;
                  int b;
                  while ((b = in.read()) >= 0) {
                    request.write(b);
                    last4 = (last4 << 8) | b;
                    if (last4 == 0x0d0a0d0a) {
                      break;
                    }
                  }
                  OutputStream out = socket.getOutputStream();
                  if (status != 200) {
                    out.write(("HTTP/1.1 " + status + " X\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                  } else {
                    out.write(("HTTP/1.1 200 OK\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    out.write(body);
                  }
                  out.flush();
                } catch (IOException e) {
                  // the test closed the connection or the server
                }
              }
            });
    thread.setDaemon(true);
    thread.start();
  }

  @After
  public void stopServer() throws Exception {
    server.close();
  }

  private HttpSdkSource source() throws Exception {
    return new HttpSdkSource("http://127.0.0.1:" + server.getLocalPort() + "/", tmp.newFolder("cache"));
  }

  private static final class Recording implements TransferProgress {
    long last;
    long total;
    int cancelAfterCalls = Integer.MAX_VALUE;
    int calls;

    @Override
    public void onProgress(long done, long totalBytes) {
      calls++;
      last = done;
      total = totalBytes;
    }

    @Override
    public boolean isCancelled() {
      return calls >= cancelAfterCalls;
    }
  }

  @Test
  public void anArchiveIsDownloadedWithProgress() throws Exception {
    Recording progress = new Recording();
    File file = source().download(new SdkArchive("platform-34_r03.zip", "0".repeat(40), body.length), progress);
    assertEquals(body.length, file.length());
    assertTrue(Arrays.equals(body, Files.readAllBytes(file.toPath())));
    assertEquals(body.length, progress.last);
    assertEquals(body.length, progress.total);
  }

  @Test
  public void aCancelledDownloadLeavesNoFile() throws Exception {
    File cache = tmp.newFolder("c2");
    HttpSdkSource source = new HttpSdkSource("http://127.0.0.1:" + server.getLocalPort() + "/", cache);
    Recording progress = new Recording();
    progress.cancelAfterCalls = 1;
    try {
      source.download(new SdkArchive("a.zip", "0".repeat(40), 1), progress);
      fail("downloaded");
    } catch (InterruptedIOException expected) {
      // expected
    }
    assertEquals(0, cache.list().length);
  }

  @Test
  public void anHttpErrorIsAFailureAndLeavesNoFile() throws Exception {
    status = 404;
    File cache = tmp.newFolder("c3");
    HttpSdkSource source = new HttpSdkSource("http://127.0.0.1:" + server.getLocalPort() + "/", cache);
    try {
      source.download(new SdkArchive("missing.zip", "0".repeat(40), 1), TransferProgress.NONE);
      fail("downloaded");
    } catch (IOException expected) {
      assertTrue(expected.getMessage().contains("404"));
    }
    assertFalse(cache.exists() && cache.list().length > 0);
  }

  @Test
  public void addressesThatCouldLeaveTheRepositoryAreRefused() throws Exception {
    HttpSdkSource source = source();
    String[] bad = {"http://evil.example/x.zip", "../x.zip", "/abs.zip", "a/../b.zip", "a b.zip", "x.zip?y=1"};
    for (String url : bad) {
      try {
        source.download(new SdkArchive(url, "0".repeat(40), 1), TransferProgress.NONE);
        fail("accepted " + url);
      } catch (IOException expected) {
        assertTrue(url, expected.getMessage().startsWith("refused address"));
      }
    }
  }

  @Test
  public void theCatalogIsOpenedFromTheRepository() throws Exception {
    try (InputStream in = source().openCatalog()) {
      assertEquals(body.length, in.readAllBytes().length);
    }
  }
}
