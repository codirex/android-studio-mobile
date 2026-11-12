package org.codirex.asm.terminal.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.codirex.asm.terminal.bootstrap.BootstrapException.Reason;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class BootstrapDownloaderTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private TestServer server;
  private String base;
  private byte[] archive;
  private File directory;

  @Before
  public void setUp() throws Exception {
    archive = new byte[300_000];
    for (int i = 0; i < archive.length; i++) {
      archive[i] = (byte) (i * 31 + 7);
    }
    directory = new File(tmp.getRoot(), "downloads");
    server = new TestServer();
    base = "http://127.0.0.1:" + server.port();
  }

  /**
   * A small HTTP server on plain sockets: the unit tests of an Android library compile against
   * android.jar, which has no com.sun.net.httpserver.
   */
  private final class TestServer {
    private final ServerSocket socket =
        new ServerSocket(0, 50, InetAddress.getLoopbackAddress());

    TestServer() throws IOException {
      Thread acceptor =
          new Thread(
              () -> {
                while (!socket.isClosed()) {
                  try {
                    final Socket client = socket.accept();
                    Thread handler = new Thread(() -> serve(client));
                    handler.setDaemon(true);
                    handler.start();
                  } catch (IOException e) {
                    return;
                  }
                }
              });
      acceptor.setDaemon(true);
      acceptor.start();
    }

    int port() {
      return socket.getLocalPort();
    }

    void stop() {
      try {
        socket.close();
      } catch (IOException ignored) {
        // Already closed.
      }
    }

    private void serve(Socket client) {
      try (Socket c = client) {
        BufferedReader in =
            new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.US_ASCII));
        String request = in.readLine();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
          // Skip the headers.
        }
        String path = request == null ? "" : request.split(" ")[1];
        OutputStream out = c.getOutputStream();
        if (path.equals("/rel/bootstrap-aarch64.zip")) {
          head(out, "200 OK", "Content-Length: " + archive.length);
          out.write(archive);
        } else if (path.equals("/moved/bootstrap-aarch64.zip")) {
          head(out, "302 Found", "Location: /rel/bootstrap-aarch64.zip\r\nContent-Length: 0");
        } else if (path.equals("/short/bootstrap-aarch64.zip")) {
          // Promises more bytes than it sends, then closes the connection.
          head(out, "200 OK", "Content-Length: " + archive.length);
          out.write(archive, 0, 1000);
        } else {
          head(out, "404 Not Found", "Content-Length: 0");
        }
        out.flush();
      } catch (IOException ignored) {
        // The client went away.
      }
    }

    private void head(OutputStream out, String status, String headers) throws IOException {
      String text = "HTTP/1.1 " + status + "\r\n" + headers + "\r\nConnection: close\r\n\r\n";
      out.write(text.getBytes(StandardCharsets.US_ASCII));
    }
  }

  @After
  public void tearDown() {
    server.stop();
  }

  private String sha256(byte[] bytes) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
    StringBuilder out = new StringBuilder();
    for (byte b : digest) {
      out.append(String.format("%02x", b));
    }
    return out.toString();
  }

  private BootstrapDownloader downloader(String folder, String hash) {
    Map<BootstrapArch, String> hashes = new EnumMap<>(BootstrapArch.class);
    if (hash != null) {
      hashes.put(BootstrapArch.AARCH64, hash);
    }
    return new BootstrapDownloader(
        new BootstrapRelease(base + folder + "/", hashes), 2_000, 1_000);
  }

  private void assertFails(Reason reason, BootstrapDownloader downloader, BootstrapArch arch,
      java.util.function.BooleanSupplier cancelled) {
    try {
      downloader.download(arch, directory, null, cancelled);
      fail("expected " + reason);
    } catch (BootstrapException e) {
      assertEquals(reason, e.reason());
    }
  }

  private void assertNoFilesLeft() {
    String[] left = directory.list();
    assertTrue(Arrays.toString(left), left == null || left.length == 0);
  }

  @Test
  public void downloadsAndVerifiesTheArchive() throws Exception {
    File file = downloader("/rel", sha256(archive)).download(
        BootstrapArch.AARCH64, directory, null, null);
    assertEquals("bootstrap-aarch64.zip", file.getName());
    assertTrue(Arrays.equals(archive, Files.readAllBytes(file.toPath())));
    assertEquals(Arrays.asList("bootstrap-aarch64.zip"), Arrays.asList(directory.list()));
  }

  @Test
  public void anUpperCaseHashIsAccepted() throws Exception {
    downloader("/rel", sha256(archive).toUpperCase()).download(
        BootstrapArch.AARCH64, directory, null, null);
  }

  @Test
  public void followsARedirect() throws Exception {
    File file = downloader("/moved", sha256(archive)).download(
        BootstrapArch.AARCH64, directory, null, null);
    assertEquals(archive.length, file.length());
  }

  @Test
  public void reportsProgressWithTheTotal() throws Exception {
    final List<Long> received = new ArrayList<>();
    final long[] total = {0};
    downloader("/rel", sha256(archive)).download(
        BootstrapArch.AARCH64,
        directory,
        (bytes, size) -> {
          received.add(bytes);
          total[0] = size;
        },
        null);
    assertFalse(received.isEmpty());
    assertEquals(archive.length, (long) received.get(received.size() - 1));
    assertEquals(archive.length, total[0]);
  }

  @Test
  public void aDifferentArchiveIsDeleted() throws Exception {
    assertFails(
        Reason.CHECKSUM,
        downloader("/rel", sha256(new byte[] {1, 2, 3})),
        BootstrapArch.AARCH64,
        null);
    assertNoFilesLeft();
  }

  @Test
  public void anArchitectureWithoutAHashIsUnavailable() throws Exception {
    assertFails(Reason.UNAVAILABLE, downloader("/rel", null), BootstrapArch.AARCH64, null);
    assertFails(Reason.UNAVAILABLE, downloader("/rel", sha256(archive)), BootstrapArch.ARM, null);
  }

  @Test
  public void aMissingFileIsANetworkError() throws Exception {
    assertFails(Reason.NETWORK, downloader("/nothing", sha256(archive)), BootstrapArch.AARCH64,
        null);
    assertNoFilesLeft();
  }

  @Test
  public void aServerThatIsNotThereIsANetworkError() throws Exception {
    BootstrapDownloader gone = downloader("/rel", sha256(archive));
    server.stop();
    assertFails(Reason.NETWORK, gone, BootstrapArch.AARCH64, null);
    assertNoFilesLeft();
  }

  @Test
  public void aDownloadThatStopsEarlyIsANetworkErrorAndLeavesNothing() throws Exception {
    assertFails(Reason.NETWORK, downloader("/short", sha256(archive)), BootstrapArch.AARCH64, null);
    assertNoFilesLeft();
  }

  @Test
  public void cancellingStopsAndLeavesNothing() throws Exception {
    final AtomicInteger polls = new AtomicInteger();
    assertFails(
        Reason.CANCELLED,
        downloader("/rel", sha256(archive)),
        BootstrapArch.AARCH64,
        () -> polls.incrementAndGet() > 1);
    assertNoFilesLeft();
  }

  @Test
  public void aStalePartFileFromACrashIsReplaced() throws Exception {
    assertTrue(directory.mkdirs());
    Files.write(new File(directory, "bootstrap-aarch64.zip.part").toPath(), new byte[] {9, 9, 9});
    File file = downloader("/rel", sha256(archive)).download(
        BootstrapArch.AARCH64, directory, null, null);
    assertEquals(archive.length, file.length());
    assertFalse(new File(directory, "bootstrap-aarch64.zip.part").exists());
  }

  @Test
  public void anOldArchiveIsReplaced() throws Exception {
    assertTrue(directory.mkdirs());
    Files.write(new File(directory, "bootstrap-aarch64.zip").toPath(), new byte[] {1});
    File file = downloader("/rel", sha256(archive)).download(
        BootstrapArch.AARCH64, directory, null, null);
    assertEquals(archive.length, file.length());
  }

  @Test
  public void theDefaultReleasePinsTheAarch64Archive() {
    BootstrapRelease release = BootstrapRelease.DEFAULT;
    assertEquals(
        "https://github.com/codirex/asm-packages/releases/download/bootstrap-v0/bootstrap-aarch64.zip",
        release.urlOf(BootstrapArch.AARCH64));
    assertEquals(64, release.sha256Of(BootstrapArch.AARCH64).length());
    assertEquals(null, release.sha256Of(BootstrapArch.X86_64));
  }

  @Test(expected = IllegalArgumentException.class)
  public void aBaseUrlWithoutASlashIsRefused() {
    new BootstrapRelease("https://example.org/x", new EnumMap<BootstrapArch, String>(BootstrapArch.class));
  }
}
