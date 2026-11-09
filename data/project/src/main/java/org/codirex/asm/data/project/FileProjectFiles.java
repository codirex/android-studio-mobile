package org.codirex.asm.data.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codirex.asm.core.fs.FileEntry;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.filetree.ProjectFiles;
import org.codirex.asm.domain.filetree.TreeNode;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectException.Reason;
import org.codirex.asm.domain.project.ProjectName;

/** Lists project folders. Paths are resolved by {@link ProjectLocator}, so they stay inside the project. */
@Singleton
final class FileProjectFiles implements ProjectFiles {

  private final FileOperations files;
  private final ProjectLocator locator;

  @Inject
  FileProjectFiles(FileOperations files, ProjectLocator locator) {
    this.files = files;
    this.locator = locator;
  }

  @Override
  public List<TreeNode> list(ProjectName project, String relativePath) throws ProjectException {
    File directory = locator.resolve(project, relativePath);

    List<FileEntry> entries;
    try {
      entries = files.list(directory);
    } catch (IOException e) {
      throw new ProjectException(Reason.STORAGE, "cannot list " + project + "/" + relativePath, e);
    }

    List<TreeNode> nodes = new ArrayList<>(entries.size());
    for (FileEntry entry : entries) {
      if (relativePath.isEmpty() && entry.name().equals(ProjectFiles.INTERNAL_DIRECTORY)) {
        continue;
      }
      String path = relativePath.isEmpty() ? entry.name() : relativePath + "/" + entry.name();
      nodes.add(new TreeNode(path, entry.isDirectory()));
    }
    return nodes;
  }
}
