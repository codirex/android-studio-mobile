package org.codirex.asm.domain.template;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.codirex.asm.domain.project.ProjectType;

/**
 * The catalog the IDE offers: the built-in templates first (the first one stays the default), then
 * the templates the user made. A custom template whose id is already taken by an earlier one is
 * left out, so a custom template never replaces a built-in one. If the custom store cannot be read
 * the built-in templates are still offered.
 */
public final class LayeredTemplateCatalog implements TemplateCatalog {

  private final TemplateCatalog builtIn;
  private final CustomTemplateStore custom;

  public LayeredTemplateCatalog(TemplateCatalog builtIn, CustomTemplateStore custom) {
    this.builtIn = Objects.requireNonNull(builtIn, "builtIn");
    this.custom = Objects.requireNonNull(custom, "custom");
  }

  @Override
  public List<Template> templatesFor(ProjectType type) {
    List<Template> result = new ArrayList<>(builtIn.templatesFor(type));
    Set<String> ids = new HashSet<>();
    for (Template template : result) {
      ids.add(template.id());
    }
    List<Template> stored;
    try {
      stored = custom.load();
    } catch (TemplateException e) {
      return result;
    }
    for (Template template : stored) {
      if (template.type() == type && ids.add(template.id())) {
        result.add(template);
      }
    }
    return result;
  }
}
