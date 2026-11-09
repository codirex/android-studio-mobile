package org.codirex.asm.domain.template;

import java.util.LinkedHashMap;
import java.util.Map;
import org.codirex.asm.domain.project.ProjectSpec;

/**
 * The variables every template can use, taken from what the user asked for:
 *
 * <ul>
 *   <li>{@code projectName}: the name as typed
 *   <li>{@code projectNameKotlin}: the name safe inside a Kotlin or Java string literal
 *   <li>{@code projectNameAndroid}: the name safe as the text of an Android string resource
 *   <li>{@code packageName}: for example {@code org.codirex.demo}
 *   <li>{@code packagePath}: the package as folders, {@code org/codirex/demo}
 *   <li>{@code projectType}: the stable id of the type
 * </ul>
 */
public final class TemplateVariables {

  private TemplateVariables() {}

  public static Map<String, String> of(ProjectSpec spec) {
    String name = spec.name().value();
    Map<String, String> variables = new LinkedHashMap<>();
    variables.put("projectName", name);
    variables.put("projectNameKotlin", escapeKotlin(name));
    variables.put("projectNameAndroid", escapeAndroid(name));
    variables.put("packageName", spec.packageName().value());
    variables.put("packagePath", spec.packageName().toDirectoryPath());
    variables.put("projectType", spec.type().id());
    return variables;
  }

  /** Makes {@code text} safe between double quotes in Kotlin and Java: escapes \, " and $. */
  static String escapeKotlin(String text) {
    StringBuilder out = new StringBuilder(text.length() + 8);
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '\\' || c == '"' || c == '$') {
        out.append('\\');
      }
      out.append(c);
    }
    return out.toString();
  }

  /**
   * Makes {@code text} safe as the text of an Android string resource: escapes the XML characters,
   * quotes and apostrophes, and a leading {@code @} or {@code ?}.
   */
  static String escapeAndroid(String text) {
    StringBuilder out = new StringBuilder(text.length() + 8);
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '&':
          out.append("&amp;");
          break;
        case '<':
          out.append("&lt;");
          break;
        case '>':
          out.append("&gt;");
          break;
        case '"':
          out.append("\\\"");
          break;
        case '\'':
          out.append("\\'");
          break;
        case '\\':
          out.append("\\\\");
          break;
        default:
          if (i == 0 && (c == '@' || c == '?')) {
            out.append('\\');
          }
          out.append(c);
      }
    }
    return out.toString();
  }
}
