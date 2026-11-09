package org.codirex.asm.domain.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.domain.template.TemplateException.Reason;
import org.junit.Test;

public final class TemplateTest {

  private static final ProjectSpec SPEC =
      new ProjectSpec(
          ProjectName.of("Demo"), PackageName.of("org.codirex.demo"), ProjectType.JAVA_LIBRARY);

  private static Template template(Map<String, String> defaults, TemplateFile... files) {
    return new Template("t", "T", ProjectType.JAVA_LIBRARY, defaults, Arrays.asList(files));
  }

  @Test
  public void rendersPathsAndContents() throws Exception {
    Template template =
        template(
            Collections.<String, String>emptyMap(),
            new TemplateFile("src/${packagePath}/Lib.java", "package ${packageName};\n"));

    List<TemplateFile> rendered = template.render(SPEC);

    assertEquals(1, rendered.size());
    assertEquals("src/org/codirex/demo/Lib.java", rendered.get(0).path());
    assertEquals("package org.codirex.demo;\n", rendered.get(0).content());
  }

  @Test
  public void usesTheTemplatesOwnDefaults() throws Exception {
    Template template =
        template(
            Collections.singletonMap("agp", "9.3.1"), new TemplateFile("v.txt", "agp ${agp}"));

    assertEquals("agp 9.3.1", template.render(SPEC).get(0).content());
  }

  @Test
  public void theProjectsValuesWinOverTheDefaults() throws Exception {
    Template template =
        template(
            Collections.singletonMap("projectName", "Other"),
            new TemplateFile("n.txt", "${projectName}"));

    assertEquals("Demo", template.render(SPEC).get(0).content());
  }

  @Test
  public void keepsTheOrderOfTheFiles() throws Exception {
    Template template =
        template(
            Collections.<String, String>emptyMap(),
            new TemplateFile("b", ""),
            new TemplateFile("a", ""));

    List<TemplateFile> rendered = template.render(SPEC);

    assertEquals("b", rendered.get(0).path());
    assertEquals("a", rendered.get(1).path());
  }

  @Test
  public void anUnsafeRenderedPathFails() {
    Template template =
        template(
            Collections.singletonMap("up", ".."), new TemplateFile("${up}/escaped.txt", "x"));

    try {
      template.render(SPEC);
      fail("expected UNSAFE_PATH");
    } catch (TemplateException e) {
      assertEquals(Reason.UNSAFE_PATH, e.reason());
    }
  }

  @Test
  public void anUnknownVariableFails() {
    Template template =
        template(Collections.<String, String>emptyMap(), new TemplateFile("a.txt", "${nobody}"));

    try {
      template.render(SPEC);
      fail("expected UNKNOWN_VARIABLE");
    } catch (TemplateException e) {
      assertEquals(Reason.UNKNOWN_VARIABLE, e.reason());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void anEmptyIdIsRefused() {
    new Template(
        "",
        "T",
        ProjectType.JAVA_LIBRARY,
        Collections.<String, String>emptyMap(),
        Collections.<TemplateFile>emptyList());
  }
}
