package org.codirex.asm.data.project;

import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectException.Reason;
import org.codirex.asm.domain.project.ProjectName;

/**
 * Finds the folder of a project and files inside it. Every path goes through {@link
 * FileOperations#resolve}, so a path with {@code ..} or a symbolic link cannot leave the project.
 * A folder without project metadata is not a project.
 */
@Singleton
final class ProjectLocator {

  /** The folder inside a project that holds the IDE's own files. */
  static final String INTERNAL_DIRECTORY = ".asm";

  private final FileOperations files;
  private final File root;

  @Inject
  ProjectLocator(FileOperations files, @ProjectsRoot File root) {
    this.files = files;
    this.root = root;
  }

  /**
   * The folder of a project.
   *
   * @throws ProjectException with reason {@code NOT_FOUND} if there is no such project, or {@code
   *     STORAGE}
   */
  File projectDirectory(ProjectName project) throws ProjectException {
    File directory;
    try {
      directory = files.resolve(root, project.value());
    } catch (IOException e) {
      throw new ProjectException(Reason.STORAGE, "cannot locate " + project, e);
    }
    if (!new File(directory, FileProjectRepository.METADATA_PATH).isFile()) {
      throw new ProjectException(Reason.NOT_FOUND, "no such project: " + project);
    }
    return directory;
  }

  /**
   * A file or folder of a project; the empty path is the project folder itself. The result may not
   * exist.
   *
   * @throws ProjectException with reason {@code NOT_FOUND} if there is no such project, or {@code
   *     STORAGE} if the path leaves the project
   */
  File resolve(ProjectName project, String relativePath) throws ProjectException {
    File directory = projectDirectory(project);
    if (relativePath.isEmpty()) {
      return directory;
    }
    try {
      return files.resolve(directory, relativePath);
    } catch (IOException e) {
      throw new ProjectException(Reason.STORAGE, "cannot locate " + project + "/" + relativePath, e);
    }
  }

  /** Whether {@code file} is the internal folder of the project in {@code directory}, or inside it. */
  static boolean isInternal(File directory, File file) throws IOException {
    String internal = new File(directory, INTERNAL_DIRECTORY).getCanonicalPath();
    String target = file.getCanonicalPath();
    return target.equals(internal) || target.startsWith(internal + File.separator);
  }
}
