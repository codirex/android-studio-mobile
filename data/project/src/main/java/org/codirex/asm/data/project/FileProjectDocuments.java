package org.codirex.asm.data.project;

import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.editor.DocumentException;
import org.codirex.asm.domain.editor.DocumentException.Reason;
import org.codirex.asm.domain.editor.ProjectDocuments;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectName;

/**
 * Reads and writes project files as UTF-8 text. A file larger than {@link #MAX_BYTES} or with a NUL
 * character near its start is refused, and the project's internal folder is off limits.
 */
@Singleton
final class FileProjectDocuments implements ProjectDocuments {

  /** How many characters from the start are checked for NUL when deciding if a file is text. */
  private static final int BINARY_SCAN_LENGTH = 8000;

  private final FileOperations files;
  private final ProjectLocator locator;

  @Inject
  FileProjectDocuments(FileOperations files, ProjectLocator locator) {
    this.files = files;
    this.locator = locator;
  }

  @Override
  public String read(ProjectName project, String relativePath) throws DocumentException {
    File file = locate(project, relativePath);
    if (!file.isFile()) {
      throw new DocumentException(Reason.NOT_FOUND, "no such file: " + relativePath);
    }
    if (file.length() > MAX_BYTES) {
      throw new DocumentException(Reason.TOO_LARGE, "file too large: " + relativePath);
    }
    String text;
    try {
      text = files.readText(file);
    } catch (IOException e) {
      throw new DocumentException(Reason.STORAGE, "cannot read " + relativePath, e);
    }
    if (looksBinary(text)) {
      throw new DocumentException(Reason.NOT_TEXT, "not a text file: " + relativePath);
    }
    return text;
  }

  @Override
  public void write(ProjectName project, String relativePath, String text)
      throws DocumentException {
    File file = locate(project, relativePath);
    if (file.isDirectory()) {
      throw new DocumentException(Reason.NOT_ALLOWED, "is a folder: " + relativePath);
    }
    try {
      files.writeText(file, text);
    } catch (IOException e) {
      throw new DocumentException(Reason.STORAGE, "cannot write " + relativePath, e);
    }
  }

  /** Resolves the file and refuses the project's internal folder. */
  private File locate(ProjectName project, String relativePath) throws DocumentException {
    if (relativePath.isEmpty()) {
      throw new DocumentException(Reason.NOT_FOUND, "no file given");
    }
    try {
      File directory = locator.projectDirectory(project);
      File file = locator.resolve(project, relativePath);
      if (ProjectLocator.isInternal(directory, file)) {
        throw new DocumentException(Reason.NOT_ALLOWED, "internal file: " + relativePath);
      }
      return file;
    } catch (ProjectException e) {
      Reason reason =
          e.reason() == ProjectException.Reason.NOT_FOUND ? Reason.NOT_FOUND : Reason.STORAGE;
      throw new DocumentException(reason, e.getMessage(), e);
    } catch (IOException e) {
      throw new DocumentException(Reason.STORAGE, "cannot locate " + relativePath, e);
    }
  }

  private static boolean looksBinary(String text) {
    int length = Math.min(text.length(), BINARY_SCAN_LENGTH);
    for (int i = 0; i < length; i++) {
      if (text.charAt(i) == 0) {
        return true;
      }
    }
    return false;
  }
}
