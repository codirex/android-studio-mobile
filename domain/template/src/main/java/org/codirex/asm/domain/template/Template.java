package org.codirex.asm.domain.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.domain.template.TemplateException.Reason;

/**
 * What a new project of one type starts with: files whose paths and contents may use placeholders
 * (see {@link TemplateRenderer}), and default values for the variables the files use besides the
 * ones every template gets ({@link TemplateVariables}). Immutable.
 */
public final class Template {

  private final String id;
  private final String name;
  private final ProjectType type;
  private final Map<String, String> defaults;
  private final List<TemplateFile> files;

  /**
   * @param id a stable identifier, not empty
   * @param name what the user reads
   * @param type the kind of project the template creates
   * @param defaults values of the template's own variables
   * @param files the files, in the order they are written
   */
  public Template(
      String id,
      String name,
      ProjectType type,
      Map<String, String> defaults,
      List<TemplateFile> files) {
    if (Objects.requireNonNull(id, "id").isEmpty()) {
      throw new IllegalArgumentException("id must not be empty");
    }
    this.id = id;
    this.name = Objects.requireNonNull(name, "name");
    this.type = Objects.requireNonNull(type, "type");
    this.defaults = Collections.unmodifiableMap(new LinkedHashMap<>(defaults));
    this.files = Collections.unmodifiableList(new ArrayList<>(files));
  }

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }

  public ProjectType type() {
    return type;
  }

  public Map<String, String> defaults() {
    return defaults;
  }

  public List<TemplateFile> files() {
    return files;
  }

  /**
   * Renders every file for {@code spec}. The project's own values win over the template's
   * defaults. Nothing is written.
   *
   * @throws TemplateException if a placeholder is unknown or not closed, or a rendered path is not
   *     safe (see {@link TemplatePaths})
   */
  public List<TemplateFile> render(ProjectSpec spec) throws TemplateException {
    Map<String, String> variables = new HashMap<>(defaults);
    variables.putAll(TemplateVariables.of(spec));
    List<TemplateFile> rendered = new ArrayList<>(files.size());
    for (TemplateFile file : files) {
      String path = TemplateRenderer.render(file.path(), variables);
      if (!TemplatePaths.isSafe(path)) {
        throw new TemplateException(Reason.UNSAFE_PATH, "unsafe path: " + path);
      }
      rendered.add(new TemplateFile(path, TemplateRenderer.render(file.content(), variables)));
    }
    return rendered;
  }
}
