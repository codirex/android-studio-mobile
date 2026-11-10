package org.codirex.asm.data.project;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.editor.DocumentException;
import org.codirex.asm.domain.editor.DocumentException.Reason;
import org.codirex.asm.domain.editor.EditorTabs;
import org.codirex.asm.domain.editor.WorkspaceStore;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectName;

/**
 * Keeps the open tabs in {@code .asm/workspace.properties} inside the project: {@code tabs} is the
 * number of tabs, {@code tab.0}, {@code tab.1} and so on are the paths, and {@code active} is the
 * active one. A file that is missing or cannot be understood gives no tabs.
 */
@Singleton
final class FileWorkspaceStore implements WorkspaceStore {

  static final String WORKSPACE_PATH = ".asm/workspace.properties";

  /** A project never has more tabs than this; a larger number in the file is damage. */
  private static final int MAX_TABS = 200;

  private final FileOperations files;
  private final ProjectLocator locator;

  @Inject
  FileWorkspaceStore(FileOperations files, ProjectLocator locator) {
    this.files = files;
    this.locator = locator;
  }

  @Override
  public EditorTabs load(ProjectName project) throws DocumentException {
    File file = new File(directory(project), WORKSPACE_PATH);
    if (!file.isFile()) {
      return EditorTabs.empty();
    }
    String text;
    try {
      text = files.readText(file);
    } catch (IOException e) {
      return EditorTabs.empty();
    }
    try {
      Properties properties = new Properties();
      properties.load(new StringReader(text));
      int count = Math.min(Integer.parseInt(properties.getProperty("tabs", "0").trim()), MAX_TABS);
      List<String> paths = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        paths.add(properties.getProperty("tab." + i));
      }
      return EditorTabs.restore(paths, properties.getProperty("active"));
    } catch (IOException | IllegalArgumentException damaged) {
      return EditorTabs.empty();
    }
  }

  @Override
  public void save(ProjectName project, EditorTabs tabs) throws DocumentException {
    File file = new File(directory(project), WORKSPACE_PATH);
    Properties properties = new Properties();
    properties.setProperty("tabs", Integer.toString(tabs.paths().size()));
    for (int i = 0; i < tabs.paths().size(); i++) {
      properties.setProperty("tab." + i, tabs.paths().get(i));
    }
    if (tabs.activePath() != null) {
      properties.setProperty("active", tabs.activePath());
    }
    try {
      StringWriter out = new StringWriter();
      properties.store(out, null);
      files.writeText(file, out.toString());
    } catch (IOException e) {
      throw new DocumentException(Reason.STORAGE, "cannot save the open tabs of " + project, e);
    }
  }

  private File directory(ProjectName project) throws DocumentException {
    try {
      return locator.projectDirectory(project);
    } catch (ProjectException e) {
      Reason reason =
          e.reason() == ProjectException.Reason.NOT_FOUND ? Reason.NOT_FOUND : Reason.STORAGE;
      throw new DocumentException(reason, e.getMessage(), e);
    }
  }
}
