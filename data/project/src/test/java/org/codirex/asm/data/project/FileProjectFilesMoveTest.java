package org.codirex.asm.data.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codirex.asm.core.fs.FileOperations;
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

public final class FileProjectFilesMoveTest {

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

  private File inProject(String path) {
    return new File(root, "Demo/" + path);
  }

  private void assertMoveFails(Reason expected, String path, String target) {
    try {
      projectFiles.move(demo, path, target);
      fail("expected " + expected);
    } catch (ProjectException e) {
      assertEquals(expected, e.reason());
    }
  }

  @Test
  public void movesAFileIntoAFolder() throws Exception {
    files.writeText(inProject("a.txt"), "hello");
    files.createDirectories(inProject("dir"));

    projectFiles.move(demo, "a.txt", "dir");

    assertFalse(inProject("a.txt").exists());
    assertEquals("hello", files.readText(inProject("dir/a.txt")));
  }

  @Test
  public void movesAFolderWithItsContentToTheRoot() throws Exception {
    files.writeText(inProject("x/y/f.txt"), "1");

    projectFiles.move(demo, "x/y", "");

    assertFalse(inProject("x/y").exists());
    assertEquals("1", files.readText(inProject("y/f.txt")));
  }

  @Test
  public void movesAFileFromAFolderToTheRoot() throws Exception {
    files.writeText(inProject("dir/a.txt"), "x");

    projectFiles.move(demo, "dir/a.txt", "");

    assertTrue(inProject("a.txt").isFile());
    assertFalse(inProject("dir/a.txt").exists());
  }

  @Test
  public void aTakenNameIsRefusedAndNothingChanges() throws Exception {
    files.writeText(inProject("a.txt"), "new");
    files.writeText(inProject("dir/a.txt"), "old");

    assertMoveFails(Reason.ALREADY_EXISTS, "a.txt", "dir");

    assertEquals("new", files.readText(inProject("a.txt")));
    assertEquals("old", files.readText(inProject("dir/a.txt")));
  }

  @Test
  public void aFolderCannotMoveIntoItselfOrIntoWhatIsInsideIt() throws Exception {
    files.createDirectories(inProject("d/sub"));

    assertMoveFails(Reason.STORAGE, "d", "d");
    assertMoveFails(Reason.STORAGE, "d", "d/sub");
    assertTrue(inProject("d/sub").isDirectory());
  }

  @Test
  public void theTargetMustBeAnExistingFolder() throws Exception {
    files.writeText(inProject("a.txt"), "x");
    files.writeText(inProject("b.txt"), "y");

    assertMoveFails(Reason.STORAGE, "a.txt", "b.txt");
    assertMoveFails(Reason.STORAGE, "a.txt", "missing");
    assertTrue(inProject("a.txt").isFile());
  }

  @Test
  public void aMissingSourceAndTheRootAreRefused() throws Exception {
    files.createDirectories(inProject("dir"));

    assertMoveFails(Reason.STORAGE, "nope.txt", "dir");
    assertMoveFails(Reason.STORAGE, "", "dir");
  }

  @Test
  public void theInternalFolderIsOffLimitsAsSourceAndAsTarget() throws Exception {
    files.writeText(inProject("a.txt"), "x");
    files.createDirectories(inProject("dir"));

    assertMoveFails(Reason.STORAGE, ".asm/project.properties", "dir");
    assertMoveFails(Reason.STORAGE, ".asm", "dir");
    assertMoveFails(Reason.STORAGE, "a.txt", ".asm");
    assertMoveFails(Reason.STORAGE, "dir/../a.txt", "dir/../.asm");
    assertTrue(inProject("a.txt").isFile());
  }

  @Test
  public void aPathThatLeavesTheProjectIsRefused() throws Exception {
    files.writeText(inProject("a.txt"), "x");
    files.createDirectories(new File(root, "Other"));

    assertMoveFails(Reason.STORAGE, "a.txt", "../Other");
    assertFalse(new File(root, "Other/a.txt").exists());
  }

  @Test
  public void anUnknownProjectIsNotFound() {
    try {
      projectFiles.move(ProjectName.of("Nope"), "a.txt", "");
      fail("expected NOT_FOUND");
    } catch (ProjectException e) {
      assertEquals(Reason.NOT_FOUND, e.reason());
    }
  }

  @Test
  public void listsAllFoldersAtAnyDepthWithoutTheInternalOne() throws Exception {
    files.createDirectories(inProject("a/b"));
    files.createDirectories(inProject("c"));
    files.writeText(inProject("a/file.txt"), "x");

    List<String> folders = new ArrayList<>(projectFiles.folders(demo));
    Collections.sort(folders);

    assertEquals(java.util.Arrays.asList("a", "a/b", "c"), folders);
  }

  @Test
  public void aProjectWithoutFoldersListsNone() throws Exception {
    assertTrue(projectFiles.folders(demo).isEmpty());
  }
}
