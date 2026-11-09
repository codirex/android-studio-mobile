package org.codirex.asm.core.fs;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Low-level file access for the {@code data:*} modules.
 *
 * <p><b>Every method blocks on disk.</b> Call them from a Clotho job on an I/O lane, never from the
 * main thread. They use {@code java.io} only, because {@code java.nio.file} is not available on API
 * 24 to 25.
 */
public interface FileOperations {

  /**
   * The implementation for the device's local file system. Production code receives it through
   * Hilt; use this in tests and in code that runs outside the dependency graph.
   */
  static FileOperations local() {
    return new LocalFileOperations();
  }

  /**
   * Lists a directory: folders first, then files, each group sorted by name ignoring case.
   *
   * @throws IOException if {@code directory} is not a readable directory
   */
  List<FileEntry> list(File directory) throws IOException;

  /**
   * Reads a whole file as UTF-8 text.
   *
   * @throws IOException if the file cannot be read
   */
  String readText(File file) throws IOException;

  /**
   * Replaces the content of {@code file} with {@code text}, encoded as UTF-8. The write is atomic: a
   * crash or a full disk leaves the old content untouched, never half of the new. Missing parent
   * directories are created.
   *
   * @throws IOException if the file cannot be written
   */
  void writeText(File file, String text) throws IOException;

  /**
   * Creates {@code directory} and any missing parents. Succeeds if it already exists.
   *
   * @return {@code directory}
   * @throws IOException if it cannot be created or exists as a file
   */
  File createDirectories(File directory) throws IOException;

  /**
   * Deletes a file, or a directory with everything inside it. A symbolic link is removed as a link;
   * what it points to is never touched. Deleting something that does not exist succeeds.
   *
   * @throws IOException if something could not be deleted
   */
  void delete(File fileOrDirectory) throws IOException;

  /**
   * Moves or renames a file or a directory with everything inside it. Nothing is overwritten and no
   * parent directory is created.
   *
   * @throws IOException if {@code source} does not exist, {@code target} exists, {@code target} lies
   *     inside {@code source}, or the move failed
   */
  void move(File source, File target) throws IOException;

  /**
   * Resolves {@code relativePath} against {@code root} and guarantees the result stays inside
   * {@code root}, even through {@code ..} segments or symbolic links. Use it for every path that
   * comes from a template, an archive or a user.
   *
   * @return the canonical file
   * @throws IOException if the path is absolute or escapes {@code root}
   */
  File resolve(File root, String relativePath) throws IOException;
}
