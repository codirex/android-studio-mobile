package org.codirex.asm.data.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

  @Test
  public void createsAnEmptyFileAtTheRoot() throws Exception {
    projectFiles.create(demo, "", "notes.txt", false);

    File created = new File(root, "Demo/notes.txt");
    assertTrue(created.isFile());
    assertEquals(0, created.length());
  }

  @Test
  public void createsAFolderInsideAnotherFolder() throws Exception {
    files.createDirectories(new File(root, "Demo/src"));

    projectFiles.create(demo, "src", "main", true);

    assertTrue(new File(root, "Demo/src/main").isDirectory());
  }

  @Test
  public void creatingAnExistingNameFailsAndKeepsTheContent() throws Exception {
    files.writeText(new File(root, "Demo/a.txt"), "keep");

    assertCreateFails(Reason.ALREADY_EXISTS, "", "a.txt", false);
    assertCreateFails(Reason.ALREADY_EXISTS, "", "a.txt", true);
    assertEquals("keep", files.readText(new File(root, "Demo/a.txt")));
  }

  @Test
  public void creatingWithAnInvalidNameIsRefused() {
    assertCreateFails(Reason.STORAGE, "", "", false);
    assertCreateFails(Reason.STORAGE, "", "..", true);
    assertCreateFails(Reason.STORAGE, "", "a/b", false);
  }

  @Test
  public void creatingInsideAFileOrAMissingFolderIsRefused() throws Exception {
    files.writeText(new File(root, "Demo/a.txt"), "x");

    assertCreateFails(Reason.STORAGE, "a.txt", "b.txt", false);
    assertCreateFails(Reason.STORAGE, "nothing", "b.txt", false);
  }

  @Test
  public void creatingOutsideTheProjectOrInTheInternalFolderIsRefused() throws Exception {
    files.createDirectories(new File(root, "Other"));

    assertCreateFails(Reason.STORAGE, "../Other", "x.txt", false);
    assertCreateFails(Reason.STORAGE, ".asm", "x.txt", false);
    assertCreateFails(Reason.STORAGE, "", ".asm", true);
    assertFalse(new File(root, "Other/x.txt").exists());
    assertFalse(new File(root, "Demo/.asm/x.txt").exists());
  }

  @Test
  public void creatingInAnUnknownProjectIsNotFound() {
    try {
      projectFiles.create(ProjectName.of("Nope"), "", "a.txt", false);
      fail("expected NOT_FOUND");
    } catch (ProjectException e) {
      assertEquals(Reason.NOT_FOUND, e.reason());
    }
  }

  @Test
  public void deletesAFile() throws Exception {
    files.writeText(new File(root, "Demo/a.txt"), "x");

    projectFiles.delete(demo, "a.txt");

    assertFalse(new File(root, "Demo/a.txt").exists());
  }

  @Test
  public void deletesAFolderWithItsContent() throws Exception {
    files.writeText(new File(root, "Demo/src/main/App.java"), "x");

    projectFiles.delete(demo, "src");

    assertFalse(new File(root, "Demo/src").exists());
    assertTrue(new File(root, "Demo/.asm/project.properties").isFile());
  }

  @Test
  public void deletingTheRootOrTheInternalFolderIsRefused() {
    assertDeleteFails("");
    assertDeleteFails(".asm");
    assertDeleteFails(".asm/project.properties");
    assertDeleteFails("src/../.asm");
    assertTrue(new File(root, "Demo/.asm/project.properties").isFile());
  }

  @Test
  public void deletingOutsideTheProjectIsRefused() throws Exception {
    files.writeText(new File(root, "Other/a.txt"), "keep");

    assertDeleteFails("../Other/a.txt");
    assertDeleteFails("../Other");
    assertTrue(new File(root, "Other/a.txt").isFile());
  }

  private void assertCreateFails(Reason expected, String parent, String name, boolean directory) {
    try {
      projectFiles.create(demo, parent, name, directory);
      fail("expected " + expected);
    } catch (ProjectException e) {
      assertEquals(expected, e.reason());
    }
  }

  private void assertDeleteFails(String path) {
    try {
      projectFiles.delete(demo, path);
      fail("expected failure for '" + path + "'");
    } catch (ProjectException e) {
      assertEquals(Reason.STORAGE, e.reason());
    }
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
