package org.codirex.asm.terminal.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.codirex.asm.terminal.bootstrap.BootstrapArch;
import org.codirex.asm.terminal.bootstrap.BootstrapException;
import org.codirex.asm.terminal.bootstrap.BootstrapException.Reason;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class UserlandSetupTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private File files;
  private File work;
  private final AtomicInteger downloads = new AtomicInteger();
  private volatile Reason downloadFailure;
  private volatile Reason installFailure;
  private volatile CountDownLatch holdDownload;
  private volatile String installed;

  @Before
  public void setUp() throws Exception {
    files = tmp.newFolder("files");
    work = new File(tmp.getRoot(), "work");
  }

  private File fakeDownload(
      BootstrapArch arch,
      File directory,
      org.codirex.asm.terminal.bootstrap.BootstrapDownloader.Progress progress,
      java.util.function.BooleanSupplier cancelled)
      throws BootstrapException, InterruptedException, java.io.IOException {
    downloads.incrementAndGet();
    assertTrue(directory.isDirectory() || directory.mkdirs());
    if (holdDownload != null) {
      while (!cancelled.getAsBoolean()) {
        if (holdDownload.await(20, TimeUnit.MILLISECONDS)) {
          break;
        }
      }
      if (cancelled.getAsBoolean()) {
        throw new BootstrapException(Reason.CANCELLED, "stopped");
      }
    }
    if (downloadFailure != null) {
      throw new BootstrapException(downloadFailure, "download failed");
    }
    progress.onProgress(50, 100);
    progress.onProgress(100, 100);
    File archive = new File(directory, arch.archiveName());
    Files.write(archive.toPath(), ("archive-" + arch.id()).getBytes("UTF-8"));
    return archive;
  }

  private UserlandSetup setup(String... abis) {
    return new UserlandSetup(
        files,
        work,
        abis,
        (arch, directory, progress, cancelled) -> {
          try {
            return fakeDownload(arch, directory, progress, cancelled);
          } catch (InterruptedException | java.io.IOException e) {
            throw new BootstrapException(Reason.STORAGE, String.valueOf(e));
          }
        },
        new UserlandSetup.Installing() {
          @Override
          public boolean isInstalled(File directory) {
            return new File(directory, "usr").isDirectory();
          }

          @Override
          public void install(
              InputStream archive,
              File directory,
              org.codirex.asm.terminal.bootstrap.BootstrapInstaller.Progress progress)
              throws BootstrapException {
            try {
              ByteArrayOutputStream out = new ByteArrayOutputStream();
              byte[] buffer = new byte[64];
              int n;
              while ((n = archive.read(buffer)) > 0) {
                out.write(buffer, 0, n);
              }
              installed = out.toString("UTF-8");
              progress.onProgress(3, 300);
              if (installFailure != null) {
                throw new BootstrapException(installFailure, "bad archive");
              }
              assertTrue(new File(directory, "usr").mkdirs());
            } catch (java.io.IOException e) {
              throw new BootstrapException(Reason.STORAGE, "io");
            }
          }
        });
  }

  private static final class Recorder implements UserlandSetup.Listener {
    final List<String> events = Collections.synchronizedList(new ArrayList<String>());
    final CountDownLatch finished = new CountDownLatch(1);
    volatile String reason;

    @Override
    public void onProgress(int stage, long done, long total) {
      events.add("progress " + stage + " " + done + " " + total);
    }

    @Override
    public void onDone() {
      events.add("done");
      finished.countDown();
    }

    @Override
    public void onFailed(String reason, String message) {
      this.reason = reason;
      events.add("failed " + reason);
      finished.countDown();
    }

    void await() throws InterruptedException {
      assertTrue("no result", finished.await(5, TimeUnit.SECONDS));
    }
  }

  @Test
  public void anInstalledUserlandNeedsNoDownload() throws Exception {
    assertTrue(new File(files, "usr").mkdirs());
    Recorder listener = new Recorder();
    setup("arm64-v8a").start(listener);
    assertEquals(Collections.singletonList("done"), listener.events);
    assertEquals(0, downloads.get());
  }

  @Test
  public void downloadsThenInstallsAndReportsEachStage() throws Exception {
    UserlandSetup setup = setup("arm64-v8a");
    assertFalse(setup.isInstalled());
    Recorder listener = new Recorder();
    setup.start(listener);
    listener.await();
    assertEquals(
        java.util.Arrays.asList(
            "progress 0 50 100", "progress 0 100 100", "progress 1 3 -1", "done"),
        listener.events);
    assertEquals("archive-aarch64", installed);
    assertTrue(setup.isInstalled());
    assertEquals(0, work.list().length); // the archive is removed
  }

  @Test
  public void theArchitectureComesFromTheDeviceAbis() throws Exception {
    Recorder listener = new Recorder();
    setup("riscv64", "x86_64").start(listener);
    listener.await();
    assertEquals("archive-x86_64", installed);
  }

  @Test
  public void aDeviceWithoutAnArchitectureHasNoUserland() throws Exception {
    Recorder listener = new Recorder();
    setup("riscv64").start(listener);
    listener.await();
    assertEquals("UNAVAILABLE", listener.reason);
    assertEquals(0, downloads.get());
  }

  @Test
  public void aFailedDownloadIsReportedWithItsReasonAndCanBeRetried() throws Exception {
    downloadFailure = Reason.NETWORK;
    UserlandSetup setup = setup("arm64-v8a");
    Recorder first = new Recorder();
    setup.start(first);
    first.await();
    assertEquals("NETWORK", first.reason);
    assertFalse(setup.isInstalled());

    downloadFailure = null;
    Recorder second = new Recorder();
    setup.start(second);
    second.await();
    assertEquals("done", second.events.get(second.events.size() - 1));
    assertTrue(setup.isInstalled());
  }

  @Test
  public void aBrokenArchiveIsReportedAndRemoved() throws Exception {
    installFailure = Reason.INVALID_ARCHIVE;
    Recorder listener = new Recorder();
    UserlandSetup setup = setup("arm64-v8a");
    setup.start(listener);
    listener.await();
    assertEquals("INVALID_ARCHIVE", listener.reason);
    assertFalse(setup.isInstalled());
    assertEquals(0, work.list().length);
  }

  @Test
  public void callersAtTheSameTimeShareOneInstallation() throws Exception {
    holdDownload = new CountDownLatch(1);
    UserlandSetup setup = setup("arm64-v8a");
    Recorder a = new Recorder();
    Recorder b = new Recorder();
    setup.start(a);
    setup.start(b);
    holdDownload.countDown();
    a.await();
    b.await();
    assertEquals(1, downloads.get());
    assertEquals("done", a.events.get(a.events.size() - 1));
    assertEquals("done", b.events.get(b.events.size() - 1));
  }

  @Test
  public void cancellingStopsTheInstallation() throws Exception {
    holdDownload = new CountDownLatch(1); // never released
    UserlandSetup setup = setup("arm64-v8a");
    Recorder listener = new Recorder();
    setup.start(listener);
    Thread.sleep(100);
    setup.cancel();
    listener.await();
    assertEquals("CANCELLED", listener.reason);
    assertFalse(setup.isInstalled());
  }

  @Test
  public void aListenerThatThrowsDoesNotStopTheOthers() throws Exception {
    UserlandSetup setup = setup("arm64-v8a");
    holdDownload = new CountDownLatch(1);
    setup.start(
        new UserlandSetup.Listener() {
          @Override
          public void onProgress(int stage, long done, long total) {
            throw new IllegalStateException("dead UI");
          }

          @Override
          public void onDone() {
            throw new IllegalStateException("dead UI");
          }

          @Override
          public void onFailed(String reason, String message) {
            throw new IllegalStateException("dead UI");
          }
        });
    Recorder alive = new Recorder();
    setup.start(alive);
    holdDownload.countDown();
    alive.await();
    assertEquals("done", alive.events.get(alive.events.size() - 1));
  }
}
