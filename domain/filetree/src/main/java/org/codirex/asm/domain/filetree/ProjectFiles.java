package org.codirex.asm.domain.filetree;

import java.util.List;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectName;

/**
 * Reads the files of a project. The domain defines it; {@code data:project} implements it.
 *
 * <p><b>Every method blocks.</b> Callers run them in a background job, never on the main thread.
 */
public interface ProjectFiles {

  /** Name of the folder that holds the project's own metadata. It is never listed. */
  String INTERNAL_DIRECTORY = ".asm";

  /**
   * Lists the entries directly inside a folder of a project: folders first, then files, each group
   * sorted by name ignoring case.
   *
   * @param relativePath the folder, relative to the project root with {@code /} as separator; empty
   *     for the root
   * @throws ProjectException with reason {@code NOT_FOUND} if there is no such project, or {@code
   *     STORAGE} if the folder cannot be read or the path leaves the project
   */
  List<TreeNode> list(ProjectName project, String relativePath) throws ProjectException;
}
