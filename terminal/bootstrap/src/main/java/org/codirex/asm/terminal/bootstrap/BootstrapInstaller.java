package org.codirex.asm.terminal.bootstrap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.terminal.bootstrap.BootstrapException.Reason;

/**
 * Installs the userland from a bootstrap archive (ADR-0007, ADR-0022). The archive holds the
 * contents of the prefix; symbolic links are not stored in it but listed in {@code SYMLINKS.txt},
 * one per line as {@code target←path} (the format of Termux's {@code generate-bootstraps.sh}).
 *
 * <p>Everything is extracted into a staging folder and moved to the prefix in one step, so a
 * crash, a full disk or a bad archive never leaves a half installed prefix: either the prefix
 * exists and is complete, or it does not exist. Every entry name and link path is checked and may
 * not leave the staging folder.
 *
 * <p><b>{@link #install} blocks on disk and on the stream.</b> Callers run it in a background job.
 */
public final class BootstrapInstaller {

  /** The folder name of the prefix inside the app's files directory. */
  public static final String PREFIX_NAME = "usr";

  /** The folder name of the home directory inside the app's files directory. */
  public static final String HOME_NAME = "home";

  static final String STAGING_NAME = "usr-staging";
  static final String SYMLINKS_FILE = "SYMLINKS.txt";
  static final String SYMLINK_SEPARATOR = "←";

  static final int MAX_ENTRIES = 100_000;
  static final long MAX_TOTAL_BYTES = 2L * 1024 * 1024 * 1024;
  static final int MAX_SYMLINKS_BYTES = 8 * 1024 * 1024;

  /** Files under these paths get the executable bit; a zip archive does not carry it. */
  private static final String[] EXECUTABLE_PREFIXES = {
    "bin/", "libexec", "lib/apt/apt-helper", "lib/apt/methods", "etc/termux/termux-bootstrap/"
  };

  /** Receives the progress of an installation. */
  public interface Progress {
    /** Does nothing. */
    Progress NONE = (entries, bytes) -> {};

    /** Called after each file: how many entries and how many bytes were extracted so far. */
    void onProgress(int entries, long bytes);
  }

  private final FileOperations files;
  private final SymlinkCreator links;

  public BootstrapInstaller(FileOperations files, SymlinkCreator links) {
    this.files = Objects.requireNonNull(files, "files");
    this.links = Objects.requireNonNull(links, "links");
  }

  /** The prefix folder for an app files directory. */
  public static File prefixOf(File filesDirectory) {
    return new File(filesDirectory, PREFIX_NAME);
  }

  /** The home folder for an app files directory. */
  public static File homeOf(File filesDirectory) {
    return new File(filesDirectory, HOME_NAME);
  }

  /** Whether the userland is installed. The prefix only ever exists complete. */
  public boolean isInstalled(File filesDirectory) {
    return prefixOf(filesDirectory).isDirectory();
  }

  /**
   * Installs the archive in {@code stream} into {@code filesDirectory}: the prefix in {@code usr}
   * and an empty home in {@code home}. Does nothing if the userland is already installed. The
   * stream is read to the end of the archive; the caller closes it.
   *
   * @throws BootstrapException {@code INVALID_ARCHIVE} if the archive is damaged, has an unsafe
   *     entry or lacks what a bootstrap has; {@code STORAGE} if reading or writing failed
   */
  public void install(InputStream stream, File filesDirectory, Progress progress)
      throws BootstrapException {
    if (isInstalled(filesDirectory)) {
      return;
    }
    File staging = new File(filesDirectory, STAGING_NAME);
    try {
      files.delete(staging);
      files.createDirectories(staging);
      String symlinks = extract(stream, staging, progress == null ? Progress.NONE : progress);
      createSymlinks(symlinks, staging);
      files.createDirectories(new File(staging, "tmp"));
      files.createDirectories(homeOf(filesDirectory));
      files.move(staging, prefixOf(filesDirectory));
    } catch (BootstrapException e) {
      cleanUp(staging);
      throw e;
    } catch (ZipException e) {
      cleanUp(staging);
      throw new BootstrapException(Reason.INVALID_ARCHIVE, "damaged archive", e);
    } catch (IOException e) {
      cleanUp(staging);
      throw new BootstrapException(Reason.STORAGE, "cannot install the userland", e);
    }
  }

  private void cleanUp(File staging) {
    try {
      files.delete(staging);
    } catch (IOException ignored) {
      // The next install starts by deleting it again.
    }
  }

  /** Extracts the files; returns the text of {@code SYMLINKS.txt}. */
  private String extract(InputStream stream, File staging, Progress progress)
      throws IOException, BootstrapException {
    ZipInputStream zip = new ZipInputStream(stream);
    String symlinks = null;
    boolean hasBinaries = false;
    int entries = 0;
    long total = 0;
    byte[] buffer = new byte[64 * 1024];
    ZipEntry entry;
    while ((entry = zip.getNextEntry()) != null) {
      if (++entries > MAX_ENTRIES) {
        throw invalid("too many entries");
      }
      String name = cleanName(entry.getName());
      if (name.isEmpty()) {
        continue;
      }
      if (entry.isDirectory()) {
        files.createDirectories(resolve(staging, name));
        continue;
      }
      if (name.equals(SYMLINKS_FILE)) {
        symlinks = readLimited(zip, buffer);
        continue;
      }
      File target = resolve(staging, name);
      files.createDirectories(target.getParentFile());
      try (OutputStream out = new FileOutputStream(target)) {
        int read;
        while ((read = zip.read(buffer)) > 0) {
          total += read;
          if (total > MAX_TOTAL_BYTES) {
            throw invalid("the archive is larger than " + MAX_TOTAL_BYTES + " bytes");
          }
          out.write(buffer, 0, read);
        }
      }
      if (isExecutable(name)) {
        if (!target.setExecutable(true, true)) {
          throw new IOException("cannot make executable: " + name);
        }
        hasBinaries |= name.startsWith("bin/");
      }
      progress.onProgress(entries, total);
    }
    if (symlinks == null) {
      throw invalid("not a bootstrap archive: no " + SYMLINKS_FILE);
    }
    if (!hasBinaries) {
      throw invalid("not a bootstrap archive: no programs in bin/");
    }
    return symlinks;
  }

  private void createSymlinks(String text, File staging) throws IOException, BootstrapException {
    for (String line : text.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      int at = trimmed.indexOf(SYMLINK_SEPARATOR);
      if (at <= 0 || at + SYMLINK_SEPARATOR.length() >= trimmed.length()) {
        throw invalid("bad line in " + SYMLINKS_FILE + ": " + trimmed);
      }
      String target = trimmed.substring(0, at);
      String path = cleanName(trimmed.substring(at + SYMLINK_SEPARATOR.length()));
      if (target.indexOf('\0') >= 0 || path.isEmpty()) {
        throw invalid("bad line in " + SYMLINKS_FILE + ": " + trimmed);
      }
      File link = resolve(staging, path);
      files.createDirectories(link.getParentFile());
      files.delete(link);
      links.create(link, target);
    }
  }

  /** Resolves a name inside {@code root}; a name that leaves it makes the archive invalid. */
  private File resolve(File root, String name) throws BootstrapException {
    try {
      return files.resolve(root, name);
    } catch (IOException e) {
      throw new BootstrapException(Reason.INVALID_ARCHIVE, "unsafe path: " + name, e);
    }
  }

  /** Removes a leading {@code ./}; refuses absolute names, backslashes and NUL characters. */
  private static String cleanName(String raw) throws BootstrapException {
    String name = raw;
    while (name.startsWith("./")) {
      name = name.substring(2);
    }
    if (name.equals(".")) {
      return "";
    }
    if (name.startsWith("/") || name.indexOf('\\') >= 0 || name.indexOf('\0') >= 0) {
      throw invalid("unsafe path: " + raw);
    }
    return name;
  }

  private static boolean isExecutable(String name) {
    for (String prefix : EXECUTABLE_PREFIXES) {
      if (name.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private static String readLimited(ZipInputStream zip, byte[] buffer)
      throws IOException, BootstrapException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int read;
    while ((read = zip.read(buffer)) > 0) {
      if (out.size() + read > MAX_SYMLINKS_BYTES) {
        throw invalid(SYMLINKS_FILE + " is too large");
      }
      out.write(buffer, 0, read);
    }
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }

  private static BootstrapException invalid(String message) {
    return new BootstrapException(Reason.INVALID_ARCHIVE, message);
  }
}
