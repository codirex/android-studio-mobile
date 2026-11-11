package org.codirex.asm.terminal.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.codirex.asm.terminal.bootstrap.BootstrapException.Reason;

/**
 * Downloads the bootstrap archive of an architecture and checks it against the SHA-256 pinned in
 * the {@link BootstrapRelease} (ADR-0022). The bytes go to a {@code .part} file; only an archive
 * whose hash matches gets its final name, so a damaged or replaced download is never installed.
 *
 * <p><b>{@link #download} blocks on the network and on disk.</b> Callers run it in a background
 * job; a cancelled job makes {@code cancelled} return true and the download stops.
 */
public final class BootstrapDownloader {

  private static final int DEFAULT_CONNECT_TIMEOUT_MS = 15_000;
  private static final int DEFAULT_READ_TIMEOUT_MS = 30_000;

  /** Receives the progress of a download. */
  public interface Progress {
    /** Does nothing. */
    Progress NONE = (bytes, total) -> {};

    /** Bytes received so far, and the size of the archive, or -1 if the server did not say. */
    void onProgress(long bytes, long total);
  }

  private final BootstrapRelease release;
  private final int connectTimeoutMs;
  private final int readTimeoutMs;

  public BootstrapDownloader(BootstrapRelease release) {
    this(release, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
  }

  /** For tests: the milliseconds to wait for a connection and between two chunks of data. */
  BootstrapDownloader(BootstrapRelease release, int connectTimeoutMs, int readTimeoutMs) {
    this.release = Objects.requireNonNull(release, "release");
    this.connectTimeoutMs = connectTimeoutMs;
    this.readTimeoutMs = readTimeoutMs;
  }

  /**
   * Downloads the archive of {@code arch} into {@code directory}, which is created if needed.
   *
   * @param progress may be null
   * @param cancelled polled while downloading; may be null
   * @return the archive, {@code bootstrap-<arch>.zip} in {@code directory}
   * @throws BootstrapException {@code UNAVAILABLE} if there is no archive for {@code arch};
   *     {@code NETWORK} for a connection or server problem; {@code CHECKSUM} if the hash differs;
   *     {@code CANCELLED}; {@code STORAGE} if the file could not be written
   */
  public File download(
      BootstrapArch arch, File directory, Progress progress, BooleanSupplier cancelled)
      throws BootstrapException {
    String expected = release.sha256Of(arch);
    if (expected == null) {
      throw new BootstrapException(Reason.UNAVAILABLE, "no bootstrap archive for " + arch.id());
    }
    Progress report = progress == null ? Progress.NONE : progress;
    BooleanSupplier stop = cancelled == null ? () -> false : cancelled;
    if (!directory.isDirectory() && !directory.mkdirs()) {
      throw new BootstrapException(Reason.STORAGE, "cannot create " + directory);
    }
    File part = new File(directory, arch.archiveName() + ".part");
    File target = new File(directory, arch.archiveName());
    try {
      String actual = fetch(release.urlOf(arch), part, report, stop);
      if (!actual.equals(expected.toLowerCase(Locale.ROOT))) {
        throw new BootstrapException(
            Reason.CHECKSUM, "the archive is not the expected one (" + actual + ")");
      }
      if (target.exists() && !target.delete()) {
        throw new IOException("cannot replace " + target);
      }
      if (!part.renameTo(target)) {
        throw new IOException("cannot rename " + part + " to " + target);
      }
      return target;
    } catch (BootstrapException e) {
      part.delete();
      throw e;
    } catch (IOException e) {
      part.delete();
      throw new BootstrapException(Reason.STORAGE, "cannot write the archive", e);
    }
  }

  /** Writes the response body to {@code part}; returns the SHA-256 of what was written. */
  private String fetch(String url, File part, Progress progress, BooleanSupplier cancelled)
      throws IOException, BootstrapException {
    MessageDigest digest = newDigest();
    HttpURLConnection connection = null;
    try {
      try {
        connection = open(url);
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setInstanceFollowRedirects(true);
        int code = responseCode(connection);
        if (code != HttpURLConnection.HTTP_OK) {
          throw new BootstrapException(Reason.NETWORK, "the server answered " + code);
        }
        long total = connection.getContentLengthLong();
        long received = 0;
        byte[] buffer = new byte[64 * 1024];
        try (InputStream in = inputOf(connection);
            OutputStream out = new FileOutputStream(part)) {
          int read;
          while ((read = readOrFail(in, buffer)) > 0) {
            if (cancelled.getAsBoolean()) {
              throw new BootstrapException(Reason.CANCELLED, "cancelled");
            }
            digest.update(buffer, 0, read);
            out.write(buffer, 0, read);
            received += read;
            progress.onProgress(received, total);
          }
        }
        if (total >= 0 && received != total) {
          throw new BootstrapException(
              Reason.NETWORK, "the download stopped after " + received + " of " + total + " bytes");
        }
      } finally {
        if (connection != null) {
          connection.disconnect();
        }
      }
    } catch (NetworkFailure e) {
      throw new BootstrapException(Reason.NETWORK, "the download failed", e.getCause());
    }
    return hex(digest.digest());
  }

  /** Marks an IOException that happened while reading from the network, not while writing. */
  private static final class NetworkFailure extends IOException {
    private static final long serialVersionUID = 1L;

    NetworkFailure(IOException cause) {
      super(cause);
    }
  }

  private static HttpURLConnection open(String url) throws IOException {
    try {
      return (HttpURLConnection) new URL(url).openConnection();
    } catch (IOException e) {
      throw new NetworkFailure(e);
    }
  }

  private static int responseCode(HttpURLConnection connection) throws IOException {
    try {
      return connection.getResponseCode();
    } catch (IOException e) {
      throw new NetworkFailure(e);
    }
  }

  private static InputStream inputOf(HttpURLConnection connection) throws IOException {
    try {
      return connection.getInputStream();
    } catch (IOException e) {
      throw new NetworkFailure(e);
    }
  }

  private static int readOrFail(InputStream in, byte[] buffer) throws IOException {
    try {
      return in.read(buffer);
    } catch (IOException e) {
      throw new NetworkFailure(e);
    }
  }

  private static MessageDigest newDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is part of every Java platform", e);
    }
  }

  private static String hex(byte[] bytes) {
    StringBuilder out = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      out.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
    }
    return out.toString();
  }
}
