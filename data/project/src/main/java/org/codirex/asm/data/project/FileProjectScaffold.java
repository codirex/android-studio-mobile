package org.codirex.asm.data.project;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.template.ProjectScaffold;
import org.codirex.asm.domain.template.TemplateException;
import org.codirex.asm.domain.template.TemplateException.Reason;
import org.codirex.asm.domain.template.TemplateFile;
import org.codirex.asm.domain.template.TemplatePaths;

/**
 * Writes template files into a project. Every path is checked twice: by {@link TemplatePaths}, and
 * by {@link ProjectLocator}, which keeps it inside the project. The internal folder is off limits.
 */
@Singleton
final class FileProjectScaffold implements ProjectScaffold {

  private final FileOperations files;
  private final ProjectLocator locator;

  @Inject
  FileProjectScaffold(FileOperations files, ProjectLocator locator) {
    this.files = files;
    this.locator = locator;
  }

  @Override
  public void write(ProjectName project, List<TemplateFile> templateFiles)
      throws TemplateException {
    try {
      File directory = locator.projectDirectory(project);
      // Check everything before writing anything, so a bad path leaves no partial result.
      for (TemplateFile file : templateFiles) {
        target(project, directory, file.path());
      }
      for (TemplateFile file : templateFiles) {
        files.writeText(target(project, directory, file.path()), file.content());
      }
    } catch (ProjectException | IOException e) {
      throw new TemplateException(Reason.STORAGE, "cannot write the template files", e);
    }
  }

  private File target(ProjectName project, File directory, String path)
      throws TemplateException, ProjectException, IOException {
    if (!TemplatePaths.isSafe(path)) {
      throw new TemplateException(Reason.UNSAFE_PATH, "unsafe path: " + path);
    }
    File target = locator.resolve(project, path);
    if (ProjectLocator.isInternal(directory, target)) {
      throw new TemplateException(Reason.UNSAFE_PATH, "internal path: " + path);
    }
    return target;
  }
}
