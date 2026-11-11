package org.codirex.asm.data.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.domain.template.Template;
import org.codirex.asm.domain.template.TemplateException;
import org.codirex.asm.domain.template.TemplateException.Reason;
import org.codirex.asm.domain.template.TemplateFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class FileCustomTemplateStoreTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private final FileOperations files = FileOperations.local();
  private File root;
  private FileCustomTemplateStore store;

  @Before
  public void setUp() {
    root = new File(tmp.getRoot(), "templates");
    store = new FileCustomTemplateStore(files, root);
  }

  private static Template sample(String id, String name, String content) {
    Map<String, String> defaults = new LinkedHashMap<>();
    defaults.put("agpVersion", "9.3.1");
    return new Template(
        id,
        name,
        ProjectType.JAVA_LIBRARY,
        defaults,
        Arrays.asList(
            new TemplateFile("src/main/java/${packagePath}/Lib.java", content),
            new TemplateFile("README.md", "# ${projectName}\n\nline twoé عربي\n"),
            new TemplateFile("empty.txt", "")));
  }

  @Test
  public void nothingStoredGivesNoTemplates() throws Exception {
    assertTrue(store.load().isEmpty());
  }

  @Test
  public void aSavedTemplateComesBackWithEverything() throws Exception {
    Template template = sample("custom-lib", "My lib", "package ${packageName};\n\n$$ \\ tab\t\n");
    store.save(template);
    List<Template> loaded = store.load();
    assertEquals(1, loaded.size());
    Template back = loaded.get(0);
    assertEquals("custom-lib", back.id());
    assertEquals("My lib", back.name());
    assertEquals(ProjectType.JAVA_LIBRARY, back.type());
    assertEquals(template.defaults(), back.defaults());
    assertEquals(template.files(), back.files());
  }

  @Test
  public void savingAgainReplacesAndLeavesNoLeftovers() throws Exception {
    store.save(sample("custom-lib", "One", "a"));
    store.save(sample("custom-lib", "Two", "b"));
    List<Template> loaded = store.load();
    assertEquals(1, loaded.size());
    assertEquals("Two", loaded.get(0).name());
    assertEquals("b", loaded.get(0).files().get(0).content());
    assertEquals(Arrays.asList("custom-lib"), Arrays.asList(root.list()));
  }

  @Test
  public void fewerFilesAfterASaveLeaveNoOldOnes() throws Exception {
    store.save(sample("custom-lib", "One", "a"));
    store.save(
        new Template(
            "custom-lib",
            "One",
            ProjectType.JAVA_LIBRARY,
            Collections.<String, String>emptyMap(),
            Collections.singletonList(new TemplateFile("only.txt", "x"))));
    assertEquals(1, store.load().get(0).files().size());
    assertFalse(new File(root, "custom-lib/files/1").exists());
  }

  @Test
  public void deleteRemovesTheTemplateAndMissingIsFine() throws Exception {
    store.save(sample("custom-a", "A", "a"));
    store.save(sample("custom-b", "B", "b"));
    store.delete("custom-a");
    store.delete("custom-a");
    assertEquals(1, store.load().size());
    assertEquals("custom-b", store.load().get(0).id());
  }

  @Test
  public void invalidTemplatesAreNotSaved() throws Exception {
    try {
      store.save(sample("custom-lib", "n", "${unknown}"));
      fail();
    } catch (TemplateException e) {
      assertEquals(Reason.UNKNOWN_VARIABLE, e.reason());
    }
    assertFalse(root.exists() && root.list().length > 0);
  }

  @Test
  public void deleteRefusesAnIdThatCouldLeaveTheFolder() throws Exception {
    try {
      store.delete("../projects");
      fail();
    } catch (TemplateException e) {
      assertEquals(Reason.INVALID, e.reason());
    }
  }

  @Test
  public void aDamagedFolderIsLeftOutAndDoesNotHideTheOthers() throws Exception {
    store.save(sample("custom-good", "Good", "a"));
    store.save(sample("custom-bad", "Bad", "a"));
    files.writeText(new File(root, "custom-bad/template.properties"), "garbage");
    store.save(sample("custom-also-good", "Also", "a"));
    // A folder that is not a template at all.
    files.createDirectories(new File(root, "custom-empty"));
    // A template whose file content is missing.
    store.save(sample("custom-nofile", "No file", "a"));
    files.delete(new File(root, "custom-nofile/files/0"));
    List<Template> loaded = store.load();
    assertEquals(2, loaded.size());
    assertTrue(Arrays.asList("custom-good", "custom-also-good").contains(loaded.get(0).id()));
  }

  @Test
  public void aTemplateWithUnsafeStoredPathsIsLeftOut() throws Exception {
    store.save(sample("custom-lib", "Lib", "a"));
    File properties = new File(root, "custom-lib/template.properties");
    String text = files.readText(properties).replace("README.md", "../README.md");
    files.writeText(properties, text);
    assertTrue(store.load().isEmpty());
  }

  @Test
  public void anInterruptedSwapIsRecoveredOnTheNextLoad() throws Exception {
    store.save(sample("custom-lib", "Lib", "a"));
    // The crash happened after the old folder was moved aside and before the new one was moved in.
    files.move(new File(root, "custom-lib"), new File(root, ".old-custom-lib"));
    List<Template> loaded = store.load();
    assertEquals(1, loaded.size());
    assertEquals("custom-lib", loaded.get(0).id());
    assertFalse(new File(root, ".old-custom-lib").exists());
  }

  @Test
  public void aStaleOldFolderIsRemovedWhenTheNewOneExists() throws Exception {
    store.save(sample("custom-lib", "Lib", "a"));
    files.createDirectories(new File(root, ".old-custom-lib"));
    assertEquals(1, store.load().size());
    assertFalse(new File(root, ".old-custom-lib").exists());
  }
}
