package org.codirex.asm.data.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.editor.DocumentException;
import org.codirex.asm.domain.editor.DocumentException.Reason;
import org.codirex.asm.domain.editor.EditorTabs;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class FileWorkspaceStoreTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private final FileOperations files = FileOperations.local();
  private final ProjectName demo = ProjectName.of("Demo");
  private File root;
  private FileWorkspaceStore store;

  @Before
  public void setUp() throws Exception {
    root = new File(tmp.getRoot(), "projects");
    FileProjectRepository repository = new FileProjectRepository(files, root, () -> 1L);
    repository.create(
        new ProjectSpec(demo, PackageName.of("org.codirex.demo"), ProjectType.APPLICATION));
    store = new FileWorkspaceStore(files, new ProjectLocator(files, root));
  }

  private File workspaceFile() {
    return new File(root, "Demo/.asm/workspace.properties");
  }

  @Test
  public void aProjectWithNothingSavedHasNoTabs() throws Exception {
    assertEquals(EditorTabs.empty(), store.load(demo));
  }

  @Test
  public void savedTabsComeBackWithTheActiveOne() throws Exception {
    EditorTabs tabs =
        EditorTabs.empty().open("src/Main.java").open("README.md").open("b.txt").select("README.md");

    store.save(demo, tabs);

    assertEquals(tabs, store.load(demo));
  }

  @Test
  public void pathsWithSpacesAndUnicodeSurvive() throws Exception {
    EditorTabs tabs = EditorTabs.empty().open("my docs/ملف مهم.txt").open("a=b:c.txt");

    store.save(demo, tabs);

    assertEquals(tabs, store.load(demo));
  }

  @Test
  public void savingNoTabsClearsWhatWasSaved() throws Exception {
    store.save(demo, EditorTabs.empty().open("a.txt"));
    store.save(demo, EditorTabs.empty());

    assertEquals(EditorTabs.empty(), store.load(demo));
  }

  @Test
  public void theFileLivesInsideTheInternalFolder() throws Exception {
    store.save(demo, EditorTabs.empty().open("a.txt"));

    assertTrue(workspaceFile().isFile());
    assertTrue(new File(root, "Demo/.asm/project.properties").isFile());
  }

  @Test
  public void aDamagedFileGivesNoTabs() throws Exception {
    files.writeText(workspaceFile(), "tabs=not a number\ntab.0=a.txt\n");
    assertEquals(EditorTabs.empty(), store.load(demo));

    files.writeText(workspaceFile(), "tabs=1\ntab.0=\\u12\n");
    assertEquals(EditorTabs.empty(), store.load(demo));
  }

  @Test
  public void missingEntriesAreSkippedAndTheCountIsCapped() throws Exception {
    files.writeText(workspaceFile(), "tabs=3\ntab.0=a.txt\ntab.2=c.txt\nactive=c.txt\n");
    EditorTabs tabs = store.load(demo);
    assertEquals(Arrays.asList("a.txt", "c.txt"), tabs.paths());
    assertEquals("c.txt", tabs.activePath());

    files.writeText(workspaceFile(), "tabs=999999999\ntab.0=a.txt\n");
    assertEquals(Arrays.asList("a.txt"), store.load(demo).paths());
  }

  @Test
  public void anUnknownProjectIsNotFound() {
    try {
      store.load(ProjectName.of("Nope"));
      fail("expected NOT_FOUND");
    } catch (DocumentException e) {
      assertEquals(Reason.NOT_FOUND, e.reason());
    }
    try {
      store.save(ProjectName.of("Nope"), EditorTabs.empty());
      fail("expected NOT_FOUND");
    } catch (DocumentException e) {
      assertEquals(Reason.NOT_FOUND, e.reason());
    }
  }
}
