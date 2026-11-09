package org.codirex.asm.domain.template;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;
import org.junit.Test;

public final class TemplateVariablesTest {

  private static ProjectSpec spec(String name) {
    return new ProjectSpec(
        ProjectName.of(name), PackageName.of("org.codirex.demo"), ProjectType.ANDROID_LIBRARY);
  }

  @Test
  public void providesTheValuesOfTheSpec() {
    Map<String, String> variables = TemplateVariables.of(spec("Demo"));

    assertEquals("Demo", variables.get("projectName"));
    assertEquals("org.codirex.demo", variables.get("packageName"));
    assertEquals("org/codirex/demo", variables.get("packagePath"));
    assertEquals("android-library", variables.get("projectType"));
  }

  @Test
  public void escapesTheNameForKotlinStrings() {
    assertEquals("My \\$App \\\"x\\\" \\\\", TemplateVariables.escapeKotlin("My $App \"x\" \\"));
    assertEquals("Plain", TemplateVariables.escapeKotlin("Plain"));
  }

  @Test
  public void escapesTheNameForAndroidStrings() {
    assertEquals("Tom &amp; Jerry", TemplateVariables.escapeAndroid("Tom & Jerry"));
    assertEquals("a &lt;b&gt;", TemplateVariables.escapeAndroid("a <b>"));
    assertEquals("it\\'s", TemplateVariables.escapeAndroid("it's"));
    assertEquals("\\\"q\\\"", TemplateVariables.escapeAndroid("\"q\""));
    assertEquals("\\@home", TemplateVariables.escapeAndroid("@home"));
    assertEquals("\\?x", TemplateVariables.escapeAndroid("?x"));
    assertEquals("a@b", TemplateVariables.escapeAndroid("a@b"));
  }

  @Test
  public void theEscapedNamesAreInTheVariables() {
    Map<String, String> variables = TemplateVariables.of(spec("Tom & $Jerry"));

    assertEquals("Tom & \\$Jerry", variables.get("projectNameKotlin"));
    assertEquals("Tom &amp; $Jerry", variables.get("projectNameAndroid"));
  }
}
