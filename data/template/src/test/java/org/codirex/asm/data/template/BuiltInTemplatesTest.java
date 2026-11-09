package org.codirex.asm.data.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.domain.template.Template;
import org.codirex.asm.domain.template.TemplateFile;
import org.codirex.asm.domain.template.TemplatePaths;
import org.junit.Test;

public final class BuiltInTemplatesTest {

  private final BuiltInTemplates catalog = new BuiltInTemplates();

  private static ProjectSpec spec(String name, ProjectType type) {
    return new ProjectSpec(ProjectName.of(name), PackageName.of("org.codirex.demo"), type);
  }

  private static TemplateFile find(List<TemplateFile> files, String path) {
    for (TemplateFile file : files) {
      if (file.path().equals(path)) {
        return file;
      }
    }
    return null;
  }

  @Test
  public void everyProjectTypeHasATemplateOfThatType() {
    for (ProjectType type : ProjectType.values()) {
      List<Template> templates = catalog.templatesFor(type);
      assertFalse("no template for " + type, templates.isEmpty());
      for (Template template : templates) {
        assertEquals(type, template.type());
      }
    }
  }

  @Test
  public void templateIdsAreUnique() {
    Set<String> ids = new HashSet<>();
    for (ProjectType type : ProjectType.values()) {
      for (Template template : catalog.templatesFor(type)) {
        assertTrue("duplicate id " + template.id(), ids.add(template.id()));
      }
    }
  }

  @Test
  public void everyTemplateRendersWithAnAwkwardProjectName() throws Exception {
    for (ProjectType type : ProjectType.values()) {
      ProjectSpec spec = spec("Tom & $Jerry's", type);
      for (Template template : catalog.templatesFor(type)) {
        List<TemplateFile> rendered = template.render(spec);

        assertFalse(rendered.isEmpty());
        Set<String> paths = new HashSet<>();
        for (TemplateFile file : rendered) {
          assertTrue("unsafe path " + file.path(), TemplatePaths.isSafe(file.path()));
          assertTrue("duplicate path " + file.path(), paths.add(file.path()));
          assertFalse(
              "unresolved placeholder in " + file.path(), file.content().contains("${"));
        }
      }
    }
  }

  @Test
  public void theProjectNameIsEscapedWhereItIsUsed() throws Exception {
    ProjectSpec spec = spec("Tom & $Jerry's", ProjectType.APPLICATION);
    List<TemplateFile> rendered = catalog.templatesFor(ProjectType.APPLICATION).get(0).render(spec);

    assertTrue(
        find(rendered, "settings.gradle.kts")
            .content()
            .contains("rootProject.name = \"Tom & \\$Jerry's\""));
    assertTrue(
        find(rendered, "app/src/main/res/values/strings.xml")
            .content()
            .contains("<string name=\"app_name\">Tom &amp; $Jerry\\'s</string>"));
  }

  @Test
  public void theApplicationHasItsActivityInThePackageFolder() throws Exception {
    List<TemplateFile> rendered =
        catalog
            .templatesFor(ProjectType.APPLICATION)
            .get(0)
            .render(spec("Demo", ProjectType.APPLICATION));

    TemplateFile activity = find(rendered, "app/src/main/java/org/codirex/demo/MainActivity.java");
    assertNotNull(activity);
    assertTrue(activity.content().startsWith("package org.codirex.demo;\n"));
    assertTrue(
        find(rendered, "app/build.gradle.kts")
            .content()
            .contains("applicationId = \"org.codirex.demo\""));
  }

  @Test
  public void theGradlePluginRegistersItsPluginClass() throws Exception {
    List<TemplateFile> rendered =
        catalog
            .templatesFor(ProjectType.GRADLE_PLUGIN)
            .get(0)
            .render(spec("Demo", ProjectType.GRADLE_PLUGIN));

    assertNotNull(find(rendered, "src/main/java/org/codirex/demo/ExamplePlugin.java"));
    assertTrue(
        find(rendered, "build.gradle.kts")
            .content()
            .contains("implementationClass = \"org.codirex.demo.ExamplePlugin\""));
  }

  @Test
  public void theLibrariesHaveATestForTheirExampleClass() throws Exception {
    for (ProjectType type : new ProjectType[] {ProjectType.JAVA_LIBRARY, ProjectType.ANDROID_LIBRARY}) {
      List<TemplateFile> rendered =
          catalog.templatesFor(type).get(0).render(spec("Demo", type));
      boolean hasTest = false;
      for (TemplateFile file : rendered) {
        hasTest |= file.path().endsWith("/org/codirex/demo/GreeterTest.java");
      }
      assertTrue("no test in " + type, hasTest);
    }
  }
}
