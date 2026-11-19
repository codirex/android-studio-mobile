package org.codirex.asm.domain.sdk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The use cases of the SDK manager (ADR-0030): list what can be installed, accept a license,
 * install and remove a package.
 *
 * <p>Only two kinds of package are offered: the platforms ({@code platforms;android-34}, with the
 * {@code android.jar} every build compiles against) and the build tools ({@code
 * build-tools;34.0.0}). Previews and release candidates are not offered.
 *
 * <p><b>Every method blocks.</b> Callers run it in a background job.
 */
public final class SdkManager {

  private static final Pattern OFFERED =
      Pattern.compile("platforms;android-\\d+(-ext\\d+)?|build-tools;\\d+(\\.\\d+)*");

  private final SdkSource source;
  private final SdkStorage storage;

  public SdkManager(SdkSource source, SdkStorage storage) {
    this.source = Objects.requireNonNull(source, "source");
    this.storage = Objects.requireNonNull(storage, "storage");
  }

  /** Whether the IDE offers the package with this catalog path. */
  public static boolean isOffered(String path) {
    return OFFERED.matcher(path).matches();
  }

  /** The offered packages with their state: platforms first, newest first, then build tools. */
  public List<SdkEntry> list() throws SdkException {
    SdkCatalog catalog;
    try (InputStream in = source.openCatalog()) {
      catalog = CatalogParser.parse(in);
    } catch (IOException e) {
      throw new SdkException(SdkException.Reason.NETWORK, "catalog", e);
    }
    Map<String, String> installed;
    try {
      installed = storage.installed();
    } catch (IOException e) {
      throw new SdkException(SdkException.Reason.STORAGE, "installed", e);
    }
    List<SdkEntry> entries = new ArrayList<>();
    for (SdkPackage pkg : catalog.packages()) {
      if (isOffered(pkg.path())) {
        entries.add(new SdkEntry(pkg, installed.get(pkg.path()), isAccepted(pkg.license())));
      }
    }
    Collections.sort(entries, ORDER);
    return entries;
  }

  /** Records that the person accepted this license, as it is worded now. */
  public void acceptLicense(SdkLicense license) throws SdkException {
    try {
      storage.acceptLicense(license.id(), license.hash());
    } catch (IOException e) {
      throw new SdkException(SdkException.Reason.STORAGE, "license", e);
    }
  }

  /**
   * Downloads, verifies and unpacks the package.
   *
   * @throws SdkException {@code LICENSE_NOT_ACCEPTED} before anything is downloaded; {@code
   *     CHECKSUM} if the archive is not the one the catalog describes
   */
  public void install(SdkPackage pkg, TransferProgress progress) throws SdkException {
    if (!isOffered(pkg.path())) {
      throw new SdkException(SdkException.Reason.NOT_OFFERED, pkg.path());
    }
    if (!isAccepted(pkg.license())) {
      throw new SdkException(SdkException.Reason.LICENSE_NOT_ACCEPTED, pkg.license().id());
    }
    File archive = null;
    try {
      archive = source.download(pkg.archive(), progress);
      if (!pkg.archive().sha1().equals(sha1Of(archive))) {
        throw new SdkException(SdkException.Reason.CHECKSUM, pkg.path());
      }
      try {
        storage.install(pkg.path(), pkg.revision(), archive);
      } catch (IOException e) {
        throw new SdkException(SdkException.Reason.STORAGE, pkg.path(), e);
      }
    } catch (InterruptedIOException e) {
      throw new SdkException(SdkException.Reason.CANCELLED, pkg.path(), e);
    } catch (IOException e) {
      throw new SdkException(SdkException.Reason.NETWORK, pkg.path(), e);
    } finally {
      if (archive != null) {
        archive.delete();
      }
    }
  }

  /** Removes the package from the device. */
  public void remove(String path) throws SdkException {
    try {
      storage.remove(path);
    } catch (IOException e) {
      throw new SdkException(SdkException.Reason.STORAGE, path, e);
    }
  }

  private boolean isAccepted(SdkLicense license) throws SdkException {
    try {
      return license.hash().equals(storage.acceptedLicenseHash(license.id()));
    } catch (IOException e) {
      throw new SdkException(SdkException.Reason.STORAGE, "license", e);
    }
  }

  private static String sha1Of(File file) throws IOException {
    try (InputStream in = Files.newInputStream(file.toPath())) {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] buffer = new byte[64 * 1024];
      int read;
      while ((read = in.read(buffer)) > 0) {
        digest.update(buffer, 0, read);
      }
      StringBuilder hex = new StringBuilder();
      for (byte b : digest.digest()) {
        hex.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Platforms before build tools; inside a kind, the higher number first. */
  private static final Comparator<SdkEntry> ORDER =
      new Comparator<SdkEntry>() {
        @Override
        public int compare(SdkEntry a, SdkEntry b) {
          String pa = a.pkg().path();
          String pb = b.pkg().path();
          boolean platformA = pa.startsWith("platforms;");
          boolean platformB = pb.startsWith("platforms;");
          if (platformA != platformB) {
            return platformA ? -1 : 1;
          }
          return compareNumbers(numbersOf(pb), numbersOf(pa));
        }
      };

  private static long[] numbersOf(String path) {
    List<Long> numbers = new ArrayList<>();
    StringBuilder digits = new StringBuilder();
    for (int i = 0; i <= path.length(); i++) {
      char c = i < path.length() ? path.charAt(i) : ' ';
      if (c >= '0' && c <= '9') {
        digits.append(c);
      } else if (digits.length() > 0) {
        numbers.add(Long.parseLong(digits.toString()));
        digits.setLength(0);
      }
    }
    long[] result = new long[numbers.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = numbers.get(i);
    }
    return result;
  }

  private static int compareNumbers(long[] a, long[] b) {
    for (int i = 0; i < Math.max(a.length, b.length); i++) {
      long x = i < a.length ? a[i] : 0;
      long y = i < b.length ? b[i] : 0;
      if (x != y) {
        return x < y ? -1 : 1;
      }
    }
    return 0;
  }
}
