package org.codirex.asm.core.fs;

import java.io.File;
import java.util.Objects;

/** One item of a directory listing. Immutable snapshot taken when the directory was listed. */
public final class FileEntry {

  private final File file;
  private final boolean directory;
  private final long size;
  private final long lastModified;

  public FileEntry(File file, boolean directory, long size, long lastModified) {
    this.file = Objects.requireNonNull(file, "file");
    this.directory = directory;
    this.size = size;
    this.lastModified = lastModified;
  }

  public File file() {
    return file;
  }

  public String name() {
    return file.getName();
  }

  public boolean isDirectory() {
    return directory;
  }

  /** Size in bytes; meaningless for directories. */
  public long size() {
    return size;
  }

  /** Milliseconds since the epoch. */
  public long lastModified() {
    return lastModified;
  }
}
