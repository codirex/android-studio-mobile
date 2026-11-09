package org.codirex.asm.domain.editor;

import org.codirex.asm.domain.project.ProjectName;

/**
 * Reads and writes the text files of a project. The domain defines it; {@code data:project}
 * implements it.
 *
 * <p><b>Every method blocks.</b> Callers run them in a background job, never on the main thread.
 */
public interface ProjectDocuments {

  /** The largest file the editor opens, in bytes. */
  long MAX_BYTES = 2L * 1024 * 1024;

  /**
   * Reads a file as UTF-8 text.
   *
   * @param relativePath the file, relative to the project root with {@code /} as separator
   * @throws DocumentException with reason {@code NOT_FOUND}, {@code NOT_ALLOWED}, {@code NOT_TEXT},
   *     {@code TOO_LARGE} or {@code STORAGE}
   */
  String read(ProjectName project, String relativePath) throws DocumentException;

  /**
   * Replaces the content of a file, or creates it with its missing folders. The write is atomic: a
   * crash leaves the old content untouched.
   *
   * @throws DocumentException with reason {@code NOT_FOUND}, {@code NOT_ALLOWED} or {@code STORAGE}
   */
  void write(ProjectName project, String relativePath, String text) throws DocumentException;
}
