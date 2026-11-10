package org.codirex.asm.feature.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.domain.template.Template;

/**
 * The templates offered for one project type. Immutable. While the list is being loaded, {@link
 * #items} is empty and the screen shows no template choice.
 */
final class TemplateChoices {

  /** One template as the screen needs it. */
  static final class Item {
    private final String id;
    private final String name;

    Item(String id, String name) {
      this.id = id;
      this.name = name;
    }

    String id() {
      return id;
    }

    String name() {
      return name;
    }
  }

  private final ProjectType type;
  private final List<Item> items;

  private TemplateChoices(ProjectType type, List<Item> items) {
    this.type = type;
    this.items = Collections.unmodifiableList(items);
  }

  /** Nothing asked for yet. */
  static TemplateChoices none() {
    return new TemplateChoices(null, new ArrayList<Item>());
  }

  /** The templates of {@code type} are being loaded. */
  static TemplateChoices loading(ProjectType type) {
    return new TemplateChoices(type, new ArrayList<Item>());
  }

  static TemplateChoices of(ProjectType type, List<Template> templates) {
    List<Item> items = new ArrayList<>(templates.size());
    for (Template template : templates) {
      items.add(new Item(template.id(), template.name()));
    }
    return new TemplateChoices(type, items);
  }

  /** The type these choices are for, or {@code null} before any was asked for. */
  ProjectType type() {
    return type;
  }

  List<Item> items() {
    return items;
  }
}
