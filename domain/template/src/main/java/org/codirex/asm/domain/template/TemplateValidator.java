package org.codirex.asm.domain.template;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.template.TemplateException.Reason;

/**
 * The rules a template must meet before it is saved. A template that passes can be rendered for any
 * project: it is rendered here once with a sample project, so an unknown placeholder or an unsafe
 * path is found when the template is saved and not when someone creates a project from it.
 */
public final class TemplateValidator {

  public static final int MAX_ID_LENGTH = 64;
  public static final int MAX_NAME_LENGTH = 80;
  public static final int MAX_FILES = 500;
  public static final int MAX_TOTAL_CHARACTERS = 2_000_000;

  private TemplateValidator() {}

  /**
   * @throws TemplateException with reason {@code INVALID} if a rule is broken, or the reason of
   *     the rendering error ({@code UNKNOWN_VARIABLE}, {@code MALFORMED}, {@code UNSAFE_PATH})
   */
  public static void validate(Template template) throws TemplateException {
    if (!isValidId(template.id())) {
      throw invalid("id must be 1 to 64 of a-z, 0-9, '.', '_' or '-', starting with a letter or digit");
    }
    String name = template.name().trim();
    if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
      throw invalid("name must be 1 to " + MAX_NAME_LENGTH + " characters");
    }
    Set<String> reserved = TemplateVariables.of(sample(template)).keySet();
    for (String variable : template.defaults().keySet()) {
      if (!isValidVariable(variable)) {
        throw invalid("bad variable name: " + variable);
      }
      if (reserved.contains(variable)) {
        throw invalid("variable is set by the project: " + variable);
      }
    }
    if (template.files().size() > MAX_FILES) {
      throw invalid("more than " + MAX_FILES + " files");
    }
    long characters = 0;
    for (TemplateFile file : template.files()) {
      characters += file.path().length() + (long) file.content().length();
    }
    for (Map.Entry<String, String> entry : template.defaults().entrySet()) {
      characters += entry.getKey().length() + (long) entry.getValue().length();
    }
    if (characters > MAX_TOTAL_CHARACTERS) {
      throw invalid("larger than " + MAX_TOTAL_CHARACTERS + " characters");
    }
    Set<String> paths = new HashSet<>();
    for (TemplateFile file : template.render(sample(template))) {
      if (!paths.add(file.path())) {
        throw invalid("two files write " + file.path());
      }
    }
  }

  /** Whether {@code id} can be used as the id of a template. */
  public static boolean isValidId(String id) {
    if (id == null || id.isEmpty() || id.length() > MAX_ID_LENGTH) {
      return false;
    }
    for (int i = 0; i < id.length(); i++) {
      char c = id.charAt(i);
      boolean alphanumeric = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
      if (!alphanumeric && (i == 0 || (c != '.' && c != '_' && c != '-'))) {
        return false;
      }
    }
    return true;
  }

  /** Whether {@code name} can be a template variable: a letter, then letters, digits or '_'. */
  public static boolean isValidVariable(String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
      boolean digit = c >= '0' && c <= '9';
      if (!(letter || (i > 0 && (digit || c == '_')))) {
        return false;
      }
    }
    return true;
  }

  private static ProjectSpec sample(Template template) {
    return new ProjectSpec(
        ProjectName.of("Sample"), PackageName.of("org.example.sample"), template.type());
  }

  private static TemplateException invalid(String message) {
    return new TemplateException(Reason.INVALID, message);
  }
}
