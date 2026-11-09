package org.codirex.asm.data.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.filetree.TreeNode;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectException.Reason;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class FileProjectFilesTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private final FileOperations files = FileOperations.local();
  private final ProjectName demo = ProjectName.of("Demo");
  private File root;
  private FileProjectFiles projectFiles;

  @Before
  public void setUp() throws Exception {
    root = new File(tmp.getRoot(), "projects");
    FileProjectRepository repository = new FileProjectRepository(files, root, () -> 1L);
    repository.create(
        new ProjectSpec(demo, PackageName.of("org.codirex.demo"), ProjectType.APPLICATION));
    projectFiles = new FileProjectFiles(files, new ProjectLocator(files, root));
  }

  private static List<String> describe(List<TreeNode> nodes) {
    List<String> result = new ArrayList<>();
    for (TreeNode node : nodes) {
      result.add((node.isDirectory() ? "d:" : "f:") + node.path());
    }
    return result;
  }

  @Test
  public void theRootListsFoldersFirstAndHidesTheInternalFolder() throws Exception {
    files.writeText(new File(root, "Demo/zeta.txt"), "z");
    files.writeText(new File(root, "Demo/Alpha.txt"), "a");
    files.createDirectories(new File(root, "Demo/src"));
    files.createDirectories(new File(root, "Demo/Build"));

    List<TreeNode> nodes = projectFiles.list(demo, "");

    assertEquals(
        Arrays.asList("d:Build", "d:src", "f:Alpha.txt", "f:zeta.txt"), describe(nodes));
  }

  @Test
  public void aNestedFolderListsItsEntriesWithFullRelativePaths() throws Exception {
    files.writeText(new File(root, "Demo/src/main/App.java"), "class App {}");
    files.createDirectories(new File(root, "Demo/src/main/res"));

    List<TreeNode> nodes = projectFiles.list(demo, "src/main");

    assertEquals(Arrays.asList("d:src/main/res", "f:src/main/App.java"), describe(nodes));
  }

  @Test
  public void theInternalFolderIsOnlyHiddenAtTheRoot() throws Exception {
    files.writeText(new File(root, "Demo/src/.asm"), "not the internal folder");

    assertEquals(Arrays.asList("f:src/.asm"), describe(projectFiles.list(demo, "src")));
  }

  @Test
  public void anEmptyProjectHasNoEntries() throws Exception {
    assertEquals(0, projectFiles.list(demo, "").size());
  }

  @Test
  public void anUnknownProjectIsNotFound() {
    assertFails(Reason.NOT_FOUND, ProjectName.of("Nope"), "");
  }

  @Test
  public void aFolderWithoutMetadataIsNotAProject() throws Exception {
    files.createDirectories(new File(root, "Plain"));
    assertFails(Reason.NOT_FOUND, ProjectName.of("Plain"), "");
  }

  @Test
  public void aMissingFolderFailsWithStorage() {
    assertFails(Reason.STORAGE, demo, "nothing/here");
  }

  @Test
  public void aPathThatLeavesTheProjectIsRefused() throws Exception {
    files.createDirectories(new File(root, "Other"));
    assertFails(Reason.STORAGE, demo, "../Other");
    assertFails(Reason.STORAGE, demo, "src/../../Other");
  }

  private void assertFails(Reason expected, ProjectName project, String path) {
    try {
      projectFiles.list(project, path);
      fail("expected " + expected);
    } catch (ProjectException e) {
      assertEquals(expected, e.reason());
    }
  }
}
