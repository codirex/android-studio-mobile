package org.codirex.asm.core.fs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** The production implementation, on the device's local file system. */
@Singleton
final class LocalFileOperations implements FileOperations {

  private static final Comparator<FileEntry> ORDER =
      (a, b) -> {
        if (a.isDirectory() != b.isDirectory()) {
          return a.isDirectory() ? -1 : 1;
        }
        int byName = String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name());
        return byName != 0 ? byName : a.name().compareTo(b.name());
      };

  @Inject
  LocalFileOperations() {}

  @Override
  public List<FileEntry> list(File directory) throws IOException {
    File[] children = directory.listFiles();
    if (children == null) {
      throw new IOException("cannot list " + directory);
    }
    List<FileEntry> entries = new ArrayList<>(children.length);
    for (File child : children) {
      entries.add(new FileEntry(child, child.isDirectory(), child.length(), child.lastModified()));
    }
    Collections.sort(entries, ORDER);
    return entries;
  }

  @Override
  public String readText(File file) throws IOException {
    try (InputStream in = new FileInputStream(file)) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
      return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
  }

  @Override
  public void writeText(File file, String text) throws IOException {
    if (file.isDirectory()) {
      throw new IOException("is a directory: " + file);
    }
    File parent = file.getAbsoluteFile().getParentFile();
    createDirectories(parent);

    File temp = File.createTempFile(".asm-", ".tmp", parent);
    boolean moved = false;
    try {
      try (FileOutputStream out = new FileOutputStream(temp)) {
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.getFD().sync();
      }
      // rename replaces an existing file atomically on the same file system.
      if (!temp.renameTo(file)) {
        throw new IOException("cannot replace " + file);
      }
      moved = true;
    } finally {
      if (!moved && !temp.delete() && temp.exists()) {
        temp.deleteOnExit();
      }
    }
  }

  @Override
  public File createDirectories(File directory) throws IOException {
    if (directory.isDirectory()) {
      return directory;
    }
    if (!directory.mkdirs() && !directory.isDirectory()) {
      throw new IOException("cannot create directory " + directory);
    }
    return directory;
  }

  @Override
  public void delete(File fileOrDirectory) throws IOException {
    if (!fileOrDirectory.exists() && !isSymbolicLink(fileOrDirectory)) {
      return;
    }
    if (fileOrDirectory.isDirectory() && !isSymbolicLink(fileOrDirectory)) {
      File[] children = fileOrDirectory.listFiles();
      if (children == null) {
        throw new IOException("cannot list " + fileOrDirectory);
      }
      for (File child : children) {
        delete(child);
      }
    }
    if (!fileOrDirectory.delete() && (fileOrDirectory.exists() || isSymbolicLink(fileOrDirectory))) {
      throw new IOException("cannot delete " + fileOrDirectory);
    }
  }

  @Override
  public File resolve(File root, String relativePath) throws IOException {
    if (relativePath.startsWith("/") || relativePath.startsWith(File.separator)) {
      throw new IOException("absolute path not allowed: " + relativePath);
    }
    File canonicalRoot = root.getCanonicalFile();
    File target = new File(canonicalRoot, relativePath).getCanonicalFile();
    String rootPath = canonicalRoot.getPath();
    String targetPath = target.getPath();
    if (!targetPath.equals(rootPath) && !targetPath.startsWith(rootPath + File.separator)) {
      throw new IOException("path escapes the root: " + relativePath);
    }
    return target;
  }

  /** True if {@code file} itself is a symbolic link, whether or not its target exists. */
  private static boolean isSymbolicLink(File file) throws IOException {
    File parent = file.getAbsoluteFile().getParentFile();
    File candidate = parent == null ? file : new File(parent.getCanonicalFile(), file.getName());
    return !candidate.getCanonicalFile().equals(candidate.getAbsoluteFile());
  }
}
