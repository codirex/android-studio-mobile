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

/**
 * Reads project files from the projects directory. Every path is resolved through {@link
 * FileOperations#resolve}, so a path with {@code ..} or a symbolic link cannot leave the project.
 * Folders that are not projects (no metadata) are refused.
 */
@Singleton
final class FileProjectFiles implements ProjectFiles {

  private final FileOperations files;
  private final File root;

  @Inject
  FileProjectFiles(FileOperations files, @ProjectsRoot File root) {
    this.files = files;
    this.root = root;
  }

  @Override
  public List<TreeNode> list(ProjectName project, String relativePath) throws ProjectException {
    File projectDirectory;
    File directory;
    try {
      projectDirectory = files.resolve(root, project.value());
      directory =
          relativePath.isEmpty() ? projectDirectory : files.resolve(projectDirectory, relativePath);
    } catch (IOException e) {
      throw new ProjectException(Reason.STORAGE, "cannot locate " + project + "/" + relativePath, e);
    }
    if (!new File(projectDirectory, FileProjectRepository.METADATA_PATH).isFile()) {
      throw new ProjectException(Reason.NOT_FOUND, "no such project: " + project);
    }

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
