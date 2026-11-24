package org.codirex.asm.data.sdk;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.codirex.asm.domain.sdk.SdkManager;
import org.codirex.asm.domain.sdk.SdkStorage;

/**
 * Keeps the SDK in a folder laid out as the Android tools expect: {@code platforms/android-34},
 * {@code build-tools/34.0.0}. A package counts as installed when its folder holds the marker file
 * {@code .asm-package}, which is written last: the package is unpacked into a folder beside it and
 * moved into place, so an interrupted install leaves no half package. The licenses the person
 * accepted are files below {@code .asm/licenses}, and are mirrored to {@code licenses/} where the
 * Android Gradle plugin looks for them.
 */
final class FileSdkStorage implements SdkStorage {

  static final String MARKER = ".asm-package";

  private static final long MAX_UNPACKED_BYTES = 3L << 30;
  private static final int MAX_ENTRIES = 200_000;
  private static final Pattern LICENSE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

  private final File root;

  FileSdkStorage(File root) {
    this.root = root;
  }

  @Override
  public Map<String, String> installed() throws IOException {
    Map<String, String> result = new HashMap<>();
    for (String kind : new String[] {"platforms", "build-tools"}) {
      File[] folders = new File(root, kind).listFiles();
      if (folders == null) {
        continue;
      }
      for (File folder : folders) {
        File marker = new File(folder, MARKER);
        String path = kind + ";" + folder.getName();
        if (marker.isFile() && SdkManager.isOffered(path)) {
          result.put(path, readValue(marker, "revision"));
        }
      }
    }
    return result;
  }

  @Override
  public void install(String path, String revision, File archive) throws IOException {
    File target = folderOf(path);
    File parent = target.getParentFile();
    if (!parent.isDirectory() && !parent.mkdirs()) {
      throw new IOException("cannot create " + parent);
    }
    File staging = new File(parent, target.getName() + ".installing");
    deleteTree(staging);
    boolean moved = false;
    try {
      unpack(archive, staging);
      Files.write(
          new File(staging, MARKER).toPath(),
          ("path=" + path + "\nrevision=" + revision + "\n").getBytes(StandardCharsets.UTF_8));
      File old = new File(parent, target.getName() + ".old");
      deleteTree(old);
      if (target.exists() && !target.renameTo(old)) {
        throw new IOException("cannot replace " + target);
      }
      if (!staging.renameTo(target)) {
        if (old.exists()) {
          old.renameTo(target);
        }
        throw new IOException("cannot move " + staging + " to " + target);
      }
      moved = true;
      deleteTree(old);
    } finally {
      if (!moved) {
        deleteTree(staging);
      }
    }
  }

  @Override
  public void remove(String path) throws IOException {
    deleteTree(folderOf(path));
  }

  @Override
  public String acceptedLicenseHash(String licenseId) throws IOException {
    File file = licenseFile(licenseId);
    if (!file.isFile()) {
      return null;
    }
    String hash = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
    // A license accepted before the Gradle files existed is carried over when it is first read.
    mirrorForGradle(licenseId, hash);
    return hash;
  }

  @Override
  public void acceptLicense(String licenseId, String hash) throws IOException {
    File file = licenseFile(licenseId);
    File parent = file.getParentFile();
    if (!parent.isDirectory() && !parent.mkdirs()) {
      throw new IOException("cannot create " + parent);
    }
    Files.write(file.toPath(), hash.getBytes(StandardCharsets.UTF_8));
    mirrorForGradle(licenseId, hash);
  }

  /**
   * The Android Gradle plugin reads {@code <sdk>/licenses/<id>}, one hash per line, and refuses to
   * download or use SDK parts without it. What the person accepted in the app is written there too.
   * {@code android-sdk-license} also gets the hash Google publishes for its current text, because
   * the plugin compares against Google's way of hashing.
   */
  private void mirrorForGradle(String licenseId, String hash) throws IOException {
    File file = new File(root, "licenses/" + licenseId);
    List<String> lines = new ArrayList<>();
    if (file.isFile()) {
      for (String line :
          new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).split("\\R")) {
        if (!line.trim().isEmpty()) {
          lines.add(line.trim());
        }
      }
    }
    boolean changed = false;
    String[] wanted =
        "android-sdk-license".equals(licenseId)
            ? new String[] {hash, "24333f8a63b6825ea9c5514f83c2829b004d1fee"}
            : new String[] {hash};
    for (String value : wanted) {
      if (!lines.contains(value)) {
        lines.add(value);
        changed = true;
      }
    }
    if (!changed) {
      return;
    }
    File parent = file.getParentFile();
    if (!parent.isDirectory() && !parent.mkdirs()) {
      throw new IOException("cannot create " + parent);
    }
    StringBuilder text = new StringBuilder();
    for (String line : lines) {
      text.append(line).append('\n');
    }
    Files.write(file.toPath(), text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private File licenseFile(String licenseId) throws IOException {
    if (!LICENSE_ID.matcher(licenseId).matches()) {
      throw new IOException("refused license id: " + licenseId);
    }
    return new File(root, ".asm/licenses/" + licenseId);
  }

  /** {@code platforms;android-34} becomes {@code <root>/platforms/android-34}. */
  private File folderOf(String path) throws IOException {
    if (!SdkManager.isOffered(path)) {
      throw new IOException("refused package: " + path);
    }
    return new File(root, path.replace(';', '/'));
  }

  /**
   * Unpacks the zip into {@code target}. If every entry is inside one top-level folder (the
   * archives of Google are), that folder is dropped. An entry that would leave {@code target} is
   * refused (zip slip), and so is an archive that unpacks to an absurd size.
   */
  private static void unpack(File archive, File target) throws IOException {
    if (!target.mkdirs()) {
      throw new IOException("cannot create " + target);
    }
    String canonicalTarget = target.getCanonicalPath() + File.separator;
    try (ZipFile zip = new ZipFile(archive)) {
      String strip = commonRoot(zip);
      long total = 0;
      int entries = 0;
      Enumeration<? extends ZipEntry> all = zip.entries();
      while (all.hasMoreElements()) {
        ZipEntry entry = all.nextElement();
        if (++entries > MAX_ENTRIES) {
          throw new IOException("too many entries");
        }
        String name = entry.getName();
        if (!strip.isEmpty()) {
          if (!name.startsWith(strip)) {
            continue;
          }
          name = name.substring(strip.length());
        }
        if (name.isEmpty()) {
          continue;
        }
        File out = new File(target, name);
        if (!out.getCanonicalPath().startsWith(canonicalTarget)) {
          throw new IOException("entry leaves the package: " + entry.getName());
        }
        if (entry.isDirectory()) {
          if (!out.isDirectory() && !out.mkdirs()) {
            throw new IOException("cannot create " + out);
          }
          continue;
        }
        File folder = out.getParentFile();
        if (!folder.isDirectory() && !folder.mkdirs()) {
          throw new IOException("cannot create " + folder);
        }
        try (InputStream in = new BufferedInputStream(zip.getInputStream(entry));
            OutputStream sink = new FileOutputStream(out)) {
          byte[] buffer = new byte[64 * 1024];
          int read;
          while ((read = in.read(buffer)) > 0) {
            total += read;
            if (total > MAX_UNPACKED_BYTES) {
              throw new IOException("unpacks to too much data");
            }
            sink.write(buffer, 0, read);
          }
        }
      }
    }
  }

  /** {@code "android-34/"} if every entry lies in that one folder, else an empty text. */
  private static String commonRoot(ZipFile zip) {
    Set<String> roots = new HashSet<>();
    Enumeration<? extends ZipEntry> all = zip.entries();
    while (all.hasMoreElements()) {
      String name = all.nextElement().getName();
      int slash = name.indexOf('/');
      if (slash < 0) {
        return "";
      }
      roots.add(name.substring(0, slash + 1));
      if (roots.size() > 1) {
        return "";
      }
    }
    return roots.size() == 1 ? roots.iterator().next() : "";
  }

  private static String readValue(File file, String key) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith(key + "=")) {
          return line.substring(key.length() + 1).trim();
        }
      }
    }
    return "";
  }

  private static void deleteTree(File file) throws IOException {
    if (!file.exists()) {
      return;
    }
    // A link is removed, never followed.
    if (file.isDirectory() && !Files.isSymbolicLink(file.toPath())) {
      File[] children = file.listFiles();
      if (children != null) {
        List<File> list = new ArrayList<>();
        for (File child : children) {
          list.add(child);
        }
        for (File child : list) {
          deleteTree(child);
        }
      }
    }
    if (!file.delete() && file.exists()) {
      throw new IOException("cannot delete " + file);
    }
  }
}
