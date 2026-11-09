package org.codirex.asm.feature.filetree;

import org.codirex.asm.domain.project.ProjectName;

/**
 * What the file tree needs from the app. The host activity implements it, so this feature never
 * knows the editor or any other feature (ADR-0010).
 */
public interface FileTreeNavigator {

  /** The user picked a file, or created one, to open. */
  void openFile(ProjectName project, String relativePath);

  /** A file or folder was deleted. Anything showing it, or something inside it, must let go. */
  void pathDeleted(ProjectName project, String relativePath);

  /**
   * A file or folder was renamed. Anything showing it, or something inside it, must follow it to
   * the new path.
   */
  void pathRenamed(ProjectName project, String oldPath, String newPath);
}
