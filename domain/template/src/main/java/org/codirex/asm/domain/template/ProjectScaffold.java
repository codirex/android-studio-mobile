package org.codirex.asm.domain.template;

import java.util.List;
import org.codirex.asm.domain.project.ProjectName;

/**
 * Writes rendered template files into a project. The domain defines it; {@code data:project}
 * implements it.
 *
 * <p><b>Every method blocks.</b> Callers run it in a background job.
 */
public interface ProjectScaffold {

  /**
   * Writes the files, creating folders as needed and replacing files that exist.
   *
   * @throws TemplateException with reason {@code UNSAFE_PATH} if a path leaves the project or
   *     points into the internal folder, or {@code STORAGE} if writing failed
   */
  void write(ProjectName project, List<TemplateFile> files) throws TemplateException;
}
