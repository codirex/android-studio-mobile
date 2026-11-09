package org.codirex.asm.feature.filetree;

import org.codirex.asm.domain.project.ProjectName;

/**
 * What the file tree needs from the app. The host activity implements it, so this feature never
 * knows the editor or any other feature (ADR-0010).
 */
public interface FileTreeNavigator {

  /**
   * The user tapped a file.
   *
   * @param project the open project
   * @param relativePath the file's path inside the project, with {@code /} as separator
   */
  void openFile(ProjectName project, String relativePath);
}
