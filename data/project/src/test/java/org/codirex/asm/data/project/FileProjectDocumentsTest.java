package org.codirex.asm.data.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.RandomAccessFile;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.editor.DocumentException;
import org.codirex.asm.domain.editor.DocumentException.Reason;
import org.codirex.asm.domain.editor.ProjectDocuments;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class FileProjectDocumentsTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private final FileOperations files = FileOperations.local();
  private final ProjectName demo = ProjectName.of("Demo");
  private File root;
  private FileProjectDocuments documents;

  @Before
  public void setUp() throws Exception {
    root = new File(tmp.getRoot(), "projects");
    FileProjectRepository repository = new FileProjectRepository(files, root, () -> 1L);
    repository.create(
        new ProjectSpec(demo, PackageName.of("org.codirex.demo"), ProjectType.APPLICATION));
    documents = new FileProjectDocuments(files, new ProjectLocator(files, root));
  }

  @Test
  public void readsWhatWasWritten() throws Exception {
    documents.write(demo, "src/Main.java", "class Main {}");
    assertEquals("class Main {}", documents.read(demo, "src/Main.java"));
  }

  @Test
  public void writingCreatesMissingFoldersAndReplacesContent() throws Exception {
    documents.write(demo, "a/b/c.txt", "one");
    documents.write(demo, "a/b/c.txt", "two");

    assertEquals("two", documents.read(demo, "a/b/c.txt"));
    assertTrue(new File(root, "Demo/a/b").isDirectory());
  }

  @Test
  public void keepsUnicodeText() throws Exception {
    documents.write(demo, "note.txt", "مرحبا بالعالم");
    assertEquals("مرحبا بالعالم", documents.read(demo, "note.txt"));
  }

  @Test
  public void aMissingFileOrAFolderIsNotFound() throws Exception {
    files.createDirectories(new File(root, "Demo/src"));
    assertReadFails(Reason.NOT_FOUND, demo, "nope.txt");
    assertReadFails(Reason.NOT_FOUND, demo, "src");
    assertReadFails(Reason.NOT_FOUND, demo, "");
  }

  @Test
  public void anUnknownProjectIsNotFound() {
    assertReadFails(Reason.NOT_FOUND, ProjectName.of("Nope"), "a.txt");
  }

  @Test
  public void aFileWithNulCharactersIsNotText() throws Exception {
    String binary = new String(new char[] {'a', 0, 'b'});
    files.writeText(new File(root, "Demo/data.bin"), binary);
    assertReadFails(Reason.NOT_TEXT, demo, "data.bin");
  }

  @Test
  public void aFileOverTheLimitIsTooLarge() throws Exception {
    File big = new File(root, "Demo/big.txt");
    try (RandomAccessFile file = new RandomAccessFile(big, "rw")) {
      file.setLength(ProjectDocuments.MAX_BYTES + 1);
    }
    assertReadFails(Reason.TOO_LARGE, demo, "big.txt");
  }

  @Test
  public void theInternalFolderIsOffLimitsEvenThroughDotDotSegments() throws Exception {
    assertReadFails(Reason.NOT_ALLOWED, demo, ".asm/project.properties");
    assertReadFails(Reason.NOT_ALLOWED, demo, "src/../.asm/project.properties");
    assertWriteFails(Reason.NOT_ALLOWED, demo, ".asm/project.properties");
    assertWriteFails(Reason.NOT_ALLOWED, demo, "x/../.asm/new.txt");
    assertFalse(new File(root, "Demo/.asm/new.txt").exists());
  }

  @Test
  public void aPathThatLeavesTheProjectIsRefused() throws Exception {
    files.createDirectories(new File(root, "Other"));
    assertReadFails(Reason.STORAGE, demo, "../Other/a.txt");
    assertWriteFails(Reason.STORAGE, demo, "../Other/a.txt");
    assertFalse(new File(root, "Other/a.txt").exists());
  }

  @Test
  public void writingOverAFolderIsRefused() throws Exception {
    files.createDirectories(new File(root, "Demo/src"));
    assertWriteFails(Reason.NOT_ALLOWED, demo, "src");
  }

  private void assertReadFails(Reason expected, ProjectName project, String path) {
    try {
      documents.read(project, path);
      fail("expected " + expected);
    } catch (DocumentException e) {
      assertEquals(expected, e.reason());
    }
  }

  private void assertWriteFails(Reason expected, ProjectName project, String path) {
    try {
      documents.write(project, path, "x");
      fail("expected " + expected);
    } catch (DocumentException e) {
      assertEquals(expected, e.reason());
    }
  }
}
