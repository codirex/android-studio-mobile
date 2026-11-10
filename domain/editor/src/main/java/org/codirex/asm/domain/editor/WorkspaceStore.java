package org.codirex.asm.domain.editor;

import org.codirex.asm.domain.project.ProjectName;

/**
 * Remembers which files a project had open, so the editor can show them again. The domain defines
 * it; {@code data:project} implements it.
 *
 * <p><b>Every method blocks.</b> Callers run them in a background job, never on the main thread.
 */
public interface WorkspaceStore {

  /**
   * The tabs saved for the project. A project that has none saved, or whose saved tabs cannot be
   * read, gives {@link EditorTabs#empty()}.
   *
   * @throws DocumentException with reason {@code NOT_FOUND} if there is no such project, or {@code
   *     STORAGE}
   */
  EditorTabs load(ProjectName project) throws DocumentException;

  /**
   * Saves the tabs of the project, replacing what was saved.
   *
   * @throws DocumentException with reason {@code NOT_FOUND} if there is no such project, or {@code
   *     STORAGE}
   */
  void save(ProjectName project, EditorTabs tabs) throws DocumentException;
}
