package org.codirex.asm.data.sdk;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;
import org.codirex.asm.domain.sdk.SdkArchive;
import org.codirex.asm.domain.sdk.SdkSource;
import org.codirex.asm.domain.sdk.TransferProgress;

/** Downloads the catalog and the archives of the Android SDK repository over HTTPS. */
final class HttpSdkSource implements SdkSource {

  /** The repository of the Android SDK. */
  static final String REPOSITORY = "https://dl.google.com/android/repository/";

  private static final String CATALOG = "repository2-3.xml";
  private static final int CONNECT_TIMEOUT_MS = 20_000;
  private static final int READ_TIMEOUT_MS = 30_000;

  /** An archive address relative to the repository: no scheme, no way out of it. */
  private static final Pattern RELATIVE_URL = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._+-]*(/[A-Za-z0-9][A-Za-z0-9._+-]*)*");

  private final String base;
  private final File temporaryFolder;

  HttpSdkSource(String base, File temporaryFolder) {
    this.base = base;
    this.temporaryFolder = temporaryFolder;
  }

  @Override
  public InputStream openCatalog() throws IOException {
    return new BufferedInputStream(open(base + CATALOG).getInputStream());
  }

  @Override
  public File download(SdkArchive archive, TransferProgress progress) throws IOException {
    if (!RELATIVE_URL.matcher(archive.url()).matches()) {
      throw new IOException("refused address: " + archive.url());
    }
    if (!temporaryFolder.isDirectory() && !temporaryFolder.mkdirs()) {
      throw new IOException("cannot create " + temporaryFolder);
    }
    HttpURLConnection connection = open(base + archive.url());
    long total = connection.getContentLengthLong();
    if (total <= 0) {
      total = archive.size();
    }
    File target = File.createTempFile("sdk-", ".zip", temporaryFolder);
    boolean done = false;
    try (InputStream in = connection.getInputStream();
        OutputStream out = new FileOutputStream(target)) {
      byte[] buffer = new byte[64 * 1024];
      long copied = 0;
      int read;
      while ((read = in.read(buffer)) > 0) {
        if (progress.isCancelled()) {
          throw new InterruptedIOException("cancelled");
        }
        out.write(buffer, 0, read);
        copied += read;
        progress.onProgress(copied, total);
      }
      done = true;
      return target;
    } finally {
      connection.disconnect();
      if (!done) {
        target.delete();
      }
    }
  }

  private static HttpURLConnection open(String address) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
    connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
    connection.setReadTimeout(READ_TIMEOUT_MS);
    int code = connection.getResponseCode();
    if (code != HttpURLConnection.HTTP_OK) {
      connection.disconnect();
      throw new IOException("HTTP " + code + " for " + address);
    }
    return connection;
  }
}
