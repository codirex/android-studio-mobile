package org.codirex.asm.domain.filetree;

import java.util.List;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectName;

/**
 * Lists, creates, renames and deletes the files of a project. The domain defines it; {@code
 * data:project} implements it.
 *
 * <p><b>Every method blocks.</b> Callers run them in a background job, never on the main thread.
 * Paths are relative to the project root and use {@code /}; the empty path is the root.
 */
public interface ProjectFiles {

  /** The folder inside every project that holds the IDE's own files. It is never listed. */
  String INTERNAL_DIRECTORY = ".asm";

  /**
   * Lists a folder of a project: folders first, then files, each group sorted by name ignoring
   * case. The internal folder is left out of the root.
   *
   * @throws ProjectException with reason {@code NOT_FOUND} if there is no such project, or {@code
   *     STORAGE} if the folder cannot be read or the path leaves the project
   */
  List<TreeNode> list(ProjectName project, String relativePath) throws ProjectException;

  /**
   * Creates an empty file or a folder named {@code name} inside the folder {@code parentPath}.
   *
   * @throws ProjectException with reason {@code ALREADY_EXISTS} if the name is taken, {@code
   *     NOT_FOUND} if there is no such project, or {@code STORAGE} if the name is not valid (see
   *     {@link EntryNames}), the parent is not a folder, the target is the internal folder or the
   *     path leaves the project
   */
  void create(ProjectName project, String parentPath, String name, boolean directory)
      throws ProjectException;

  /**
   * Gives a file or folder a new name; it stays in the same folder, with everything inside it.
   *
   * @throws ProjectException with reason {@code ALREADY_EXISTS} if the new name is taken, {@code
   *     NOT_FOUND} if there is no such project, or {@code STORAGE} if the new name is not valid, the
   *     entry does not exist, it is the project root or the internal folder, or the path leaves the
   *     project
   */
  void rename(ProjectName project, String relativePath, String newName) throws ProjectException;

  /**
   * Deletes a file, or a folder with everything inside it. The project root and the internal folder
   * cannot be deleted.
   *
   * @throws ProjectException with reason {@code NOT_FOUND} if there is no such project, or {@code
   *     STORAGE} if the deletion failed or is not allowed
   */
  void delete(ProjectName project, String relativePath) throws ProjectException;
}
