package org.codirex.asm.data.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.domain.template.TemplateException;
import org.codirex.asm.domain.template.TemplateException.Reason;
import org.codirex.asm.domain.template.TemplateFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class FileProjectScaffoldTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private final FileOperations files = FileOperations.local();
  private final ProjectName demo = ProjectName.of("Demo");
  private File root;
  private FileProjectScaffold scaffold;

  @Before
  public void setUp() throws Exception {
    root = new File(tmp.getRoot(), "projects");
    FileProjectRepository repository = new FileProjectRepository(files, root, () -> 1L);
    repository.create(
        new ProjectSpec(demo, PackageName.of("org.codirex.demo"), ProjectType.APPLICATION));
    scaffold = new FileProjectScaffold(files, new ProjectLocator(files, root));
  }

  @Test
  public void writesFilesAndTheirFolders() throws Exception {
    scaffold.write(
        demo,
        Arrays.asList(
            new TemplateFile("README.md", "# Demo\n"),
            new TemplateFile("src/main/java/org/App.java", "class App {}\n")));

    assertEquals("# Demo\n", files.readText(new File(root, "Demo/README.md")));
    assertEquals(
        "class App {}\n", files.readText(new File(root, "Demo/src/main/java/org/App.java")));
    assertTrue(new File(root, "Demo/.asm/project.properties").isFile());
  }

  @Test
  public void keepsUnicodeContent() throws Exception {
    scaffold.write(demo, Collections.singletonList(new TemplateFile("a.txt", "مرحبا")));

    assertEquals("مرحبا", files.readText(new File(root, "Demo/a.txt")));
  }

  @Test
  public void anUnsafePathWritesNothingAtAll() throws Exception {
    assertWriteFails(
        Reason.UNSAFE_PATH,
        new TemplateFile("good.txt", "x"),
        new TemplateFile("../escaped.txt", "x"));

    assertFalse(new File(root, "Demo/good.txt").exists());
    assertFalse(new File(root, "escaped.txt").exists());
  }

  @Test
  public void theInternalFolderIsOffLimits() throws Exception {
    String before = files.readText(new File(root, "Demo/.asm/project.properties"));

    assertWriteFails(Reason.UNSAFE_PATH, new TemplateFile(".asm/project.properties", "broken"));
    assertWriteFails(Reason.UNSAFE_PATH, new TemplateFile(".asm/new.txt", "x"));

    assertEquals(before, files.readText(new File(root, "Demo/.asm/project.properties")));
    assertFalse(new File(root, "Demo/.asm/new.txt").exists());
  }

  @Test
  public void anUnknownProjectFailsWithStorage() {
    try {
      scaffold.write(ProjectName.of("Nope"), Collections.singletonList(new TemplateFile("a", "x")));
      fail("expected STORAGE");
    } catch (TemplateException e) {
      assertEquals(Reason.STORAGE, e.reason());
    }
  }

  private void assertWriteFails(Reason expected, TemplateFile... templateFiles) {
    try {
      scaffold.write(demo, Arrays.asList(templateFiles));
      fail("expected " + expected);
    } catch (TemplateException e) {
      assertEquals(expected, e.reason());
    }
  }
}
