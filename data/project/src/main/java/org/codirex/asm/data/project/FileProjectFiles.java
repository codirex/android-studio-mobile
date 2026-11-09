package org.codirex.asm.data.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codirex.asm.core.fs.FileEntry;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.filetree.EntryNames;
import org.codirex.asm.domain.filetree.ProjectFiles;
import org.codirex.asm.domain.filetree.TreeNode;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectException.Reason;
import org.codirex.asm.domain.project.ProjectName;

/**
 * Lists, creates, renames and deletes project files. Paths are resolved by {@link ProjectLocator},
 * so they stay inside the project.
 */
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

  @Override
  public void create(ProjectName project, String parentPath, String name, boolean directory)
      throws ProjectException {
    if (!EntryNames.isValid(name)) {
      throw new ProjectException(Reason.STORAGE, "invalid name: " + name);
    }
    File parent = locator.resolve(project, parentPath);
    if (!parent.isDirectory()) {
      throw new ProjectException(Reason.STORAGE, "not a folder: " + parentPath);
    }
    File target = locator.resolve(project, parentPath.isEmpty() ? name : parentPath + "/" + name);
    rejectInternal(project, target);
    if (target.exists()) {
      throw new ProjectException(Reason.ALREADY_EXISTS, "already exists: " + name);
    }
    try {
      if (directory) {
        files.createDirectories(target);
      } else {
        files.writeText(target, "");
      }
    } catch (IOException e) {
      throw new ProjectException(Reason.STORAGE, "cannot create " + name, e);
    }
  }

  @Override
  public void rename(ProjectName project, String relativePath, String newName)
      throws ProjectException {
    if (relativePath.isEmpty()) {
      throw new ProjectException(Reason.STORAGE, "the project root cannot be renamed");
    }
    if (!EntryNames.isValid(newName)) {
      throw new ProjectException(Reason.STORAGE, "invalid name: " + newName);
    }
    File source = locator.resolve(project, relativePath);
    rejectInternal(project, source);
    if (!source.exists()) {
      throw new ProjectException(Reason.STORAGE, "no such file: " + relativePath);
    }
    int slash = relativePath.lastIndexOf('/');
    String parent = slash < 0 ? "" : relativePath.substring(0, slash);
    File target = locator.resolve(project, parent.isEmpty() ? newName : parent + "/" + newName);
    rejectInternal(project, target);
    if (target.exists()) {
      throw new ProjectException(Reason.ALREADY_EXISTS, "already exists: " + newName);
    }
    try {
      files.move(source, target);
    } catch (IOException e) {
      throw new ProjectException(Reason.STORAGE, "cannot rename " + relativePath, e);
    }
  }

  @Override
  public void delete(ProjectName project, String relativePath) throws ProjectException {
    if (relativePath.isEmpty()) {
      throw new ProjectException(Reason.STORAGE, "the project root cannot be deleted");
    }
    File target = locator.resolve(project, relativePath);
    rejectInternal(project, target);
    try {
      files.delete(target);
    } catch (IOException e) {
      throw new ProjectException(Reason.STORAGE, "cannot delete " + relativePath, e);
    }
  }

  /** The internal folder is the IDE's: nothing is created in it, renamed or deleted from it. */
  private void rejectInternal(ProjectName project, File target) throws ProjectException {
    try {
      File directory = locator.projectDirectory(project);
      if (ProjectLocator.isInternal(directory, target)) {
        throw new ProjectException(Reason.STORAGE, "internal path: " + target.getName());
      }
    } catch (IOException e) {
      throw new ProjectException(Reason.STORAGE, "cannot check " + target.getName(), e);
    }
  }
}
