package org.codirex.asm.terminal.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.terminal.bootstrap.BootstrapArch;
import org.codirex.asm.terminal.bootstrap.BootstrapDownloader;
import org.codirex.asm.terminal.bootstrap.BootstrapException;
import org.codirex.asm.terminal.bootstrap.BootstrapInstaller;
import org.codirex.asm.terminal.bootstrap.BootstrapRelease;
import org.codirex.asm.terminal.bootstrap.OsSymlinkCreator;

/**
 * Downloads and installs the userland once at a time, however many callers ask (ADR-0024). The work
 * runs on its own thread; listeners are told about the progress and the end.
 */
final class UserlandSetup {

  static final int STAGE_DOWNLOAD = 0;
  static final int STAGE_INSTALL = 1;

  /** Fetches the archive of an architecture. */
  interface Downloading {
    File download(
        BootstrapArch arch,
        File directory,
        BootstrapDownloader.Progress progress,
        BooleanSupplier cancelled)
        throws BootstrapException;
  }

  /** Puts the archive in place. */
  interface Installing {
    boolean isInstalled(File filesDirectory);

    void install(InputStream archive, File filesDirectory, BootstrapInstaller.Progress progress)
        throws BootstrapException;
  }

  /** Where the progress and the result go; a listener that throws is dropped. */
  interface Listener {
    void onProgress(int stage, long done, long total);

    void onDone();

    /** {@code reason} is the name of a {@code BootstrapException.Reason}, or UNKNOWN. */
    void onFailed(String reason, String message);
  }

  private final File filesDirectory;
  private final File workDirectory;
  private final String[] abis;
  private final Downloading downloading;
  private final Installing installing;

  private final Object lock = new Object();
  private final List<Listener> listeners = new ArrayList<>();
  private boolean running;
  private volatile boolean cancelled;

  UserlandSetup(
      File filesDirectory,
      File workDirectory,
      String[] abis,
      Downloading downloading,
      Installing installing) {
    this.filesDirectory = filesDirectory;
    this.workDirectory = workDirectory;
    this.abis = abis;
    this.downloading = downloading;
    this.installing = installing;
  }

  /** The real thing: the pinned release of the project and the Android symbolic links. */
  static UserlandSetup create(File filesDirectory, File cacheDirectory, String[] abis) {
    final BootstrapDownloader downloader = new BootstrapDownloader(BootstrapRelease.DEFAULT);
    final BootstrapInstaller installer =
        new BootstrapInstaller(FileOperations.local(), new OsSymlinkCreator());
    return new UserlandSetup(
        filesDirectory,
        new File(cacheDirectory, "bootstrap"),
        abis,
        downloader::download,
        new Installing() {
          @Override
          public boolean isInstalled(File directory) {
            return installer.isInstalled(directory);
          }

          @Override
          public void install(
              InputStream archive, File directory, BootstrapInstaller.Progress progress)
              throws BootstrapException {
            installer.install(archive, directory, progress);
          }
        });
  }

  boolean isInstalled() {
    return installing.isInstalled(filesDirectory);
  }

  /**
   * Starts the installation, or joins the one that runs. If the userland is installed, the listener
   * only hears {@code onDone}.
   */
  void start(Listener listener) {
    if (isInstalled()) {
      safely(listener, Listener::onDone);
      return;
    }
    boolean launch;
    synchronized (lock) {
      listeners.add(listener);
      launch = !running;
      if (launch) {
        running = true;
        cancelled = false;
      }
    }
    if (launch) {
      Thread thread = new Thread(this::run, "userland-setup");
      thread.setDaemon(true);
      thread.start();
    }
  }

  /** Asks a running installation to stop. */
  void cancel() {
    cancelled = true;
  }

  private void run() {
    File archive = null;
    String reason = null;
    String message = null;
    try {
      BootstrapArch arch = BootstrapArch.forAbis(abis);
      if (arch == null) {
        throw new BootstrapException(
            BootstrapException.Reason.UNAVAILABLE, "no userland for this device");
      }
      archive =
          downloading.download(
              arch,
              workDirectory,
              (bytes, total) -> tell(l -> l.onProgress(STAGE_DOWNLOAD, bytes, total)),
              () -> cancelled);
      if (cancelled) {
        throw new BootstrapException(BootstrapException.Reason.CANCELLED, "cancelled");
      }
      try (InputStream stream = new FileInputStream(archive)) {
        installing.install(
            stream,
            filesDirectory,
            (entries, bytes) -> tell(l -> l.onProgress(STAGE_INSTALL, entries, -1)));
      }
    } catch (BootstrapException e) {
      reason = e.reason().name();
      message = e.getMessage();
    } catch (IOException | RuntimeException e) {
      reason = "UNKNOWN";
      message = String.valueOf(e.getMessage());
    }
    // Before the listeners hear about the end, so that a caller who reacts sees a clean state.
    if (archive != null && !archive.delete()) {
      archive.deleteOnExit();
    }
    finish(reason, message);
  }

  private void finish(String reason, String message) {
    List<Listener> done;
    synchronized (lock) {
      done = new ArrayList<>(listeners);
      listeners.clear();
      running = false;
    }
    for (Listener listener : done) {
      if (reason == null) {
        safely(listener, Listener::onDone);
      } else {
        safely(listener, l -> l.onFailed(reason, message));
      }
    }
  }

  private interface Call {
    void run(Listener listener);
  }

  private void tell(Call call) {
    List<Listener> current;
    synchronized (lock) {
      current = new ArrayList<>(listeners);
    }
    for (Listener listener : current) {
      if (!safely(listener, call)) {
        synchronized (lock) {
          listeners.remove(listener);
        }
      }
    }
  }

  private static boolean safely(Listener listener, Call call) {
    try {
      call.run(listener);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }
}
