package org.codirex.asm.feature.editor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.codirex.asm.domain.xmlassist.LibraryIndex;

/**
 * The libraries of the module that a layout belongs to (ADR-0038), read from the list that the
 * Gradle sync wrote and kept until that list changes. Reading parses the values files of every
 * library, so it must not happen on each keystroke.
 */
final class LibraryCache {

  private static final class Entry {
    final long modified;
    final LibraryIndex index;
    final long scanned;

    Entry(long modified, LibraryIndex index, long scanned) {
      this.modified = modified;
      this.index = index;
      this.scanned = scanned;
    }
  }

  private static final long SOURCES_TTL_MS = 10_000;
  private static final Map<File, Entry> ENTRIES = new HashMap<>();

  private LibraryCache() {}

  /** The module of a file: the nearest folder up to the project that holds a library list. */
  static File moduleOf(File projectDirectory, String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      file = new File(projectDirectory, path);
    }
    for (File dir = file.getParentFile(); dir != null; dir = dir.getParentFile()) {
      if (LibraryIndex.listOf(dir) != null) {
        return dir;
      }
      if (dir.equals(projectDirectory)) {
        break;
      }
    }
    return null;
  }

  /** The libraries of the module of {@code path}; empty before a sync. */
  static synchronized LibraryIndex get(File projectDirectory, String path) {
    if (projectDirectory == null || path == null) {
      return LibraryIndex.empty();
    }
    File module = moduleOf(projectDirectory, path);
    File list = module == null ? null : LibraryIndex.listOf(module);
    if (list == null) {
      return LibraryIndex.empty();
    }
    Entry entry = ENTRIES.get(module);
    if (entry == null || entry.modified != list.lastModified()) {
      entry = new Entry(list.lastModified(), LibraryIndex.load(list), System.currentTimeMillis());
      ENTRIES.put(module, entry);
    } else if (System.currentTimeMillis() - entry.scanned > SOURCES_TTL_MS) {
      // The views of the project change as the person types classes; the libraries do not.
      entry = new Entry(entry.modified, entry.index.refreshSources(), System.currentTimeMillis());
      ENTRIES.put(module, entry);
    }
    return entry.index;
  }
}
