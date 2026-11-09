package org.codirex.asm.data.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
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

public final class FileProjectFilesRenameTest {

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

  @Test
  public void renamesAFile() throws Exception {
    files.writeText(new File(root, "Demo/a.txt"), "content");

    projectFiles.rename(demo, "a.txt", "b.txt");

    assertFalse(new File(root, "Demo/a.txt").exists());
    assertEquals("content", files.readText(new File(root, "Demo/b.txt")));
  }

  @Test
  public void renamesAFolderInsideAnotherFolderWithItsContent() throws Exception {
    files.writeText(new File(root, "Demo/src/old/App.java"), "class App {}");

    projectFiles.rename(demo, "src/old", "fresh");

    assertFalse(new File(root, "Demo/src/old").exists());
    assertEquals("class App {}", files.readText(new File(root, "Demo/src/fresh/App.java")));
  }

  @Test
  public void aTakenNameFailsAndChangesNothing() throws Exception {
    files.writeText(new File(root, "Demo/a.txt"), "A");
    files.writeText(new File(root, "Demo/b.txt"), "B");

    assertRenameFails(Reason.ALREADY_EXISTS, "a.txt", "b.txt");
    assertRenameFails(Reason.ALREADY_EXISTS, "a.txt", "a.txt");

    assertEquals("A", files.readText(new File(root, "Demo/a.txt")));
    assertEquals("B", files.readText(new File(root, "Demo/b.txt")));
  }

  @Test
  public void anInvalidNewNameIsRefused() throws Exception {
    files.writeText(new File(root, "Demo/a.txt"), "A");

    assertRenameFails(Reason.STORAGE, "a.txt", "");
    assertRenameFails(Reason.STORAGE, "a.txt", "..");
    assertRenameFails(Reason.STORAGE, "a.txt", "x/y.txt");
    assertRenameFails(Reason.STORAGE, "a.txt", "../escaped.txt");

    assertTrue(new File(root, "Demo/a.txt").isFile());
    assertFalse(new File(root, "escaped.txt").exists());
  }

  @Test
  public void aMissingEntryOrTheRootIsRefused() {
    assertRenameFails(Reason.STORAGE, "nothing.txt", "x.txt");
    assertRenameFails(Reason.STORAGE, "", "x");
  }

  @Test
  public void theInternalFolderCannotBeRenamedOrTakenAsATarget() throws Exception {
    files.writeText(new File(root, "Demo/a.txt"), "A");

    assertRenameFails(Reason.STORAGE, ".asm", "other");
    assertRenameFails(Reason.STORAGE, ".asm/project.properties", "x");
    assertRenameFails(Reason.STORAGE, "a.txt", ".asm");

    assertTrue(new File(root, "Demo/.asm/project.properties").isFile());
    assertTrue(new File(root, "Demo/a.txt").isFile());
  }

  @Test
  public void anUnknownProjectIsNotFound() {
    try {
      projectFiles.rename(ProjectName.of("Nope"), "a.txt", "b.txt");
      fail("expected NOT_FOUND");
    } catch (ProjectException e) {
      assertEquals(Reason.NOT_FOUND, e.reason());
    }
  }

  private void assertRenameFails(Reason expected, String path, String newName) {
    try {
      projectFiles.rename(demo, path, newName);
      fail("expected " + expected);
    } catch (ProjectException e) {
      assertEquals(expected, e.reason());
    }
  }
}
