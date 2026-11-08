package org.codirex.asm.data.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.Project;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectException.Reason;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class FileProjectRepositoryTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private final FileOperations files = FileOperations.local();
  private final AtomicLong now = new AtomicLong(1000);
  private File root;
  private FileProjectRepository repository;

  @Before
  public void setUp() throws IOException {
    root = new File(tmp.getRoot(), "projects");
    repository = new FileProjectRepository(files, root, now::incrementAndGet);
  }

  private static ProjectSpec spec(String name, ProjectType type) {
    return new ProjectSpec(ProjectName.of(name), PackageName.of("org.codirex.demo"), type);
  }

  private void assertFails(Reason expected, ThrowingRunnable action) {
    try {
      action.run();
      fail("expected " + expected);
    } catch (ProjectException e) {
      assertEquals(expected, e.reason());
    }
  }

  private interface ThrowingRunnable {
    void run() throws ProjectException;
  }

  @Test
  public void listIsEmptyWhenTheRootDoesNotExist() throws ProjectException {
    assertTrue(repository.list().isEmpty());
  }

  @Test
  public void createWritesTheFolderAndMetadata() throws Exception {
    Project project = repository.create(spec("Demo", ProjectType.APPLICATION));

    assertEquals(ProjectName.of("Demo"), project.name());
    assertEquals(1001L, project.createdAt());
    assertTrue(new File(root, "Demo").isDirectory());
    assertTrue(new File(root, "Demo/" + FileProjectRepository.METADATA_PATH).isFile());
  }

  @Test
  public void createdProjectsCanBeFoundAndListed() throws ProjectException {
    Project created = repository.create(spec("Demo", ProjectType.ANDROID_LIBRARY));

    Optional<Project> found = repository.find(ProjectName.of("Demo"));
    assertTrue(found.isPresent());
    assertEquals(created, found.get());

    List<Project> all = repository.list();
    assertEquals(1, all.size());
    assertEquals(created, all.get(0));
  }

  @Test
  public void keepsUnicodeNames() throws ProjectException {
    Project created = repository.create(spec("تطبيقي", ProjectType.JAVA_LIBRARY));
    assertEquals(created, repository.find(ProjectName.of("تطبيقي")).get());
  }

  @Test
  public void createRejectsADuplicateName() throws ProjectException {
    repository.create(spec("Demo", ProjectType.APPLICATION));
    assertFails(
        Reason.ALREADY_EXISTS, () -> repository.create(spec("Demo", ProjectType.JAVA_LIBRARY)));
    assertEquals(ProjectType.APPLICATION, repository.find(ProjectName.of("Demo")).get().type());
  }

  @Test
  public void createRejectsAFolderThatAlreadyExistsWithoutMetadata() throws Exception {
    assertTrue(new File(root, "Taken").mkdirs());
    assertFails(
        Reason.ALREADY_EXISTS, () -> repository.create(spec("Taken", ProjectType.APPLICATION)));
  }

  @Test
  public void findIsEmptyForUnknownNamesAndPlainFolders() throws Exception {
    assertFalse(repository.find(ProjectName.of("Nope")).isPresent());
    assertTrue(new File(root, "Plain").mkdirs());
    assertFalse(repository.find(ProjectName.of("Plain")).isPresent());
  }

  @Test
  public void listIsNewestFirstAndSkipsFoldersThatAreNotProjects() throws Exception {
    repository.create(spec("First", ProjectType.APPLICATION));
    repository.create(spec("Second", ProjectType.APPLICATION));
    assertTrue(new File(root, "Plain").mkdirs());
    assertTrue(new File(root, "loose.txt").createNewFile());

    List<Project> all = repository.list();

    assertEquals(2, all.size());
    assertEquals("Second", all.get(0).name().value());
    assertEquals("First", all.get(1).name().value());
  }

  @Test
  public void brokenMetadataFailsFindButNotList() throws Exception {
    repository.create(spec("Good", ProjectType.APPLICATION));
    repository.create(spec("Bad", ProjectType.APPLICATION));
    files.writeText(new File(root, "Bad/" + FileProjectRepository.METADATA_PATH), "format=1\npackage=nope\n");

    assertFails(Reason.INVALID_METADATA, () -> repository.find(ProjectName.of("Bad")));

    List<Project> all = repository.list();
    assertEquals(1, all.size());
    assertEquals("Good", all.get(0).name().value());
  }

  @Test
  public void aNewerFormatIsRejected() throws Exception {
    repository.create(spec("Future", ProjectType.APPLICATION));
    files.writeText(
        new File(root, "Future/" + FileProjectRepository.METADATA_PATH),
        "format=2\npackage=org.codirex.demo\ntype=application\ncreated=1\n");

    assertFails(Reason.INVALID_METADATA, () -> repository.find(ProjectName.of("Future")));
  }

  @Test
  public void deleteRemovesTheProjectAndItsFiles() throws Exception {
    repository.create(spec("Demo", ProjectType.APPLICATION));
    files.writeText(new File(root, "Demo/src/Main.java"), "class Main {}");

    repository.delete(ProjectName.of("Demo"));

    assertFalse(new File(root, "Demo").exists());
    assertTrue(repository.list().isEmpty());
  }

  @Test
  public void deleteRefusesUnknownProjectsAndPlainFolders() throws Exception {
    assertFails(Reason.NOT_FOUND, () -> repository.delete(ProjectName.of("Nope")));

    File plain = new File(root, "Plain");
    assertTrue(plain.mkdirs());
    assertFails(Reason.NOT_FOUND, () -> repository.delete(ProjectName.of("Plain")));
    assertTrue(plain.exists());
  }

  @Test
  public void aFailedCreateLeavesNothingBehind() throws Exception {
    // The root exists as a file, so no project folder can be created inside it.
    assertTrue(tmp.getRoot().isDirectory());
    assertTrue(root.createNewFile());

    assertFails(Reason.STORAGE, () -> repository.create(spec("Demo", ProjectType.APPLICATION)));
    assertTrue(root.isFile());
  }
}
