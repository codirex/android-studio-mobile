package org.codirex.asm.domain.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.domain.template.TemplateException.Reason;
import org.junit.Before;
import org.junit.Test;

public final class TemplateManagerTest {

  private static final class MemoryStore implements CustomTemplateStore {
    final List<Template> templates = new ArrayList<>();
    boolean failLoad;

    @Override
    public List<Template> load() throws TemplateException {
      if (failLoad) {
        throw new TemplateException(Reason.STORAGE, "disk");
      }
      return new ArrayList<>(templates);
    }

    @Override
    public void save(Template template) {
      delete(template.id());
      templates.add(template);
    }

    @Override
    public void delete(String id) {
      templates.removeIf(t -> t.id().equals(id));
    }
  }

  private final MemoryStore store = new MemoryStore();
  private Template builtIn;
  private TemplateManager manager;

  @Before
  public void setUp() {
    builtIn =
        template("empty", "Empty", ProjectType.JAVA_LIBRARY, "src/${packagePath}/A.java", "x");
    manager = new TemplateManager(store, type -> type == builtIn.type()
        ? Collections.singletonList(builtIn) : Collections.<Template>emptyList());
  }

  private static Template template(
      String id, String name, ProjectType type, String path, String content) {
    return new Template(
        id,
        name,
        type,
        Collections.<String, String>emptyMap(),
        Collections.singletonList(new TemplateFile(path, content)));
  }

  private static void assertInvalid(Template template, Reason reason) {
    try {
      TemplateValidator.validate(template);
      fail("expected " + reason);
    } catch (TemplateException e) {
      assertEquals(reason, e.reason());
    }
  }

  @Test
  public void duplicateCopiesFilesAndStoresWithPrefixedId() throws Exception {
    Template copy = manager.duplicate(builtIn, "  My Library 2 ");
    assertEquals("custom-my-library-2", copy.id());
    assertEquals("My Library 2", copy.name());
    assertEquals(builtIn.files(), copy.files());
    assertEquals(builtIn.type(), copy.type());
    assertEquals(1, store.templates.size());
  }

  @Test
  public void duplicateNumbersAClashingId() throws Exception {
    manager.duplicate(builtIn, "Mine");
    Template second = manager.duplicate(builtIn, "Mine");
    Template third = manager.duplicate(builtIn, "mine!");
    assertEquals("custom-mine-2", second.id());
    assertEquals("custom-mine-3", third.id());
  }

  @Test
  public void nameWithoutLettersGetsAFallbackSlug() {
    assertEquals("custom-template", TemplateManager.uniqueId("!!!", new HashSet<String>()));
    assertEquals("custom-a-b", TemplateManager.uniqueId("A  b", new HashSet<String>()));
  }

  @Test
  public void longNamesStayWithinTheIdLimit() throws Exception {
    StringBuilder name = new StringBuilder();
    for (int i = 0; i < 70; i++) {
      name.append('a');
    }
    Template copy = manager.duplicate(builtIn, name.toString());
    assertTrue(copy.id().length() <= TemplateValidator.MAX_ID_LENGTH);
    assertTrue(TemplateValidator.isValidId(copy.id()));
  }

  @Test
  public void emptyNameIsRejected() throws Exception {
    try {
      manager.duplicate(builtIn, "   ");
      fail();
    } catch (TemplateException e) {
      assertEquals(Reason.INVALID, e.reason());
    }
    assertTrue(store.templates.isEmpty());
  }

  @Test
  public void saveRefusesBuiltInIds() {
    try {
      manager.save(builtIn);
      fail();
    } catch (TemplateException e) {
      assertEquals(Reason.INVALID, e.reason());
    }
    try {
      manager.delete("empty");
      fail();
    } catch (TemplateException e) {
      assertEquals(Reason.INVALID, e.reason());
    }
  }

  @Test
  public void saveReplacesAndDeleteRemoves() throws Exception {
    Template copy = manager.duplicate(builtIn, "Mine");
    Template changed =
        new Template(copy.id(), "Renamed", copy.type(), copy.defaults(), copy.files());
    manager.save(changed);
    assertEquals(1, manager.custom().size());
    assertEquals("Renamed", manager.custom().get(0).name());
    manager.delete(copy.id());
    assertTrue(manager.custom().isEmpty());
  }

  @Test
  public void validatorChecksIdAndName() {
    assertInvalid(template("Upper", "n", ProjectType.APPLICATION, "a", ""), Reason.INVALID);
    assertInvalid(template("-x", "n", ProjectType.APPLICATION, "a", ""), Reason.INVALID);
    assertInvalid(template("a b", "n", ProjectType.APPLICATION, "a", ""), Reason.INVALID);
    assertInvalid(template("ok", "  ", ProjectType.APPLICATION, "a", ""), Reason.INVALID);
    assertTrue(TemplateValidator.isValidId("a.b_c-1"));
    assertFalse(TemplateValidator.isValidId(""));
  }

  @Test
  public void validatorFindsRenderingProblemsAtSaveTime() {
    assertInvalid(
        template("ok", "n", ProjectType.APPLICATION, "a.txt", "${nope}"),
        Reason.UNKNOWN_VARIABLE);
    assertInvalid(
        template("ok", "n", ProjectType.APPLICATION, "a.txt", "${projectName"), Reason.MALFORMED);
    assertInvalid(
        template("ok", "n", ProjectType.APPLICATION, "../a.txt", ""), Reason.UNSAFE_PATH);
    assertInvalid(
        template("ok", "n", ProjectType.APPLICATION, ".asm/x", ""), Reason.UNSAFE_PATH);
  }

  @Test
  public void validatorRejectsDuplicatePathsAfterRendering() {
    Template t =
        new Template(
            "ok",
            "n",
            ProjectType.APPLICATION,
            Collections.<String, String>emptyMap(),
            Arrays.asList(
                new TemplateFile("${projectName}.txt", "a"), new TemplateFile("Sample.txt", "b")));
    assertInvalid(t, Reason.INVALID);
  }

  @Test
  public void validatorChecksDefaults() {
    Map<String, String> bad = new LinkedHashMap<>();
    bad.put("1x", "v");
    assertInvalid(withDefaults(bad), Reason.INVALID);
    Map<String, String> shadow = new LinkedHashMap<>();
    shadow.put("packageName", "v");
    assertInvalid(withDefaults(shadow), Reason.INVALID);
    Map<String, String> good = new LinkedHashMap<>();
    good.put("agpVersion", "9.3.1");
    good.put("a_1", "v");
    try {
      TemplateValidator.validate(withDefaults(good));
    } catch (TemplateException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void validatorLimitsFilesAndSize() {
    List<TemplateFile> many = new ArrayList<>();
    for (int i = 0; i <= TemplateValidator.MAX_FILES; i++) {
      many.add(new TemplateFile("f" + i, ""));
    }
    assertInvalid(
        new Template("ok", "n", ProjectType.APPLICATION, Collections.<String, String>emptyMap(), many),
        Reason.INVALID);
    char[] big = new char[TemplateValidator.MAX_TOTAL_CHARACTERS + 1];
    Arrays.fill(big, 'x');
    assertInvalid(
        template("ok", "n", ProjectType.APPLICATION, "a", new String(big)), Reason.INVALID);
  }

  @Test
  public void variableNames() {
    assertTrue(TemplateValidator.isValidVariable("a"));
    assertTrue(TemplateValidator.isValidVariable("A_1"));
    assertFalse(TemplateValidator.isValidVariable("_a"));
    assertFalse(TemplateValidator.isValidVariable("a-b"));
    assertFalse(TemplateValidator.isValidVariable(""));
    assertFalse(TemplateValidator.isValidVariable(null));
  }

  @Test
  public void layeredCatalogListsBuiltInsFirstThenCustomOfTheSameType() throws Exception {
    Template mine = manager.duplicate(builtIn, "Mine");
    store.templates.add(
        template("custom-app", "App", ProjectType.APPLICATION, "a", ""));
    TemplateCatalog catalog = new LayeredTemplateCatalog(
        type -> type == builtIn.type()
            ? Collections.singletonList(builtIn) : Collections.<Template>emptyList(),
        store);
    List<Template> library = catalog.templatesFor(ProjectType.JAVA_LIBRARY);
    assertEquals(Arrays.asList(builtIn, mine), library);
    assertEquals(1, catalog.templatesFor(ProjectType.APPLICATION).size());
    assertEquals("custom-app", catalog.templatesFor(ProjectType.APPLICATION).get(0).id());
  }

  @Test
  public void layeredCatalogNeverLetsACustomTemplateReplaceABuiltInOne() {
    store.templates.add(template("empty", "Fake", ProjectType.JAVA_LIBRARY, "a", ""));
    TemplateCatalog catalog = new LayeredTemplateCatalog(
        type -> Collections.singletonList(builtIn), store);
    assertEquals(Collections.singletonList(builtIn), catalog.templatesFor(ProjectType.JAVA_LIBRARY));
  }

  @Test
  public void layeredCatalogSurvivesAFailingStore() {
    store.failLoad = true;
    TemplateCatalog catalog = new LayeredTemplateCatalog(
        type -> Collections.singletonList(builtIn), store);
    assertEquals(Collections.singletonList(builtIn), catalog.templatesFor(ProjectType.JAVA_LIBRARY));
  }

  private static Template withDefaults(Map<String, String> defaults) {
    return new Template(
        "ok",
        "n",
        ProjectType.APPLICATION,
        defaults,
        Collections.singletonList(new TemplateFile("a", "")));
  }
}
