package org.codirex.asm.domain.template;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.domain.template.TemplateException.Reason;

/**
 * The use cases of the template manager: list the user's templates, copy any template (a built-in
 * one too) into a new custom template, save a changed one and delete one. Everything is checked
 * with {@link TemplateValidator} before it is stored.
 *
 * <p><b>Every method blocks.</b> Callers run it in a background job.
 */
public final class TemplateManager {

  /** Custom ids start with this, so they never meet the id of a built-in template. */
  public static final String ID_PREFIX = "custom-";

  private final CustomTemplateStore store;
  private final TemplateCatalog builtIn;

  public TemplateManager(CustomTemplateStore store, TemplateCatalog builtIn) {
    this.store = Objects.requireNonNull(store, "store");
    this.builtIn = Objects.requireNonNull(builtIn, "builtIn");
  }

  /** The templates the user made. */
  public List<Template> custom() throws TemplateException {
    return new ArrayList<>(store.load());
  }

  /**
   * Copies {@code source} into a new custom template named {@code name} and stores it. The id is
   * made from the name and is unique among the stored templates and the built-in ones.
   */
  public Template duplicate(Template source, String name) throws TemplateException {
    String trimmed = name.trim();
    Set<String> taken = new HashSet<>();
    for (Template template : store.load()) {
      taken.add(template.id());
    }
    for (ProjectType type : ProjectType.values()) {
      for (Template template : builtIn.templatesFor(type)) {
        taken.add(template.id());
      }
    }
    Template copy =
        new Template(
            uniqueId(trimmed, taken), trimmed, source.type(), source.defaults(), source.files());
    TemplateValidator.validate(copy);
    store.save(copy);
    return copy;
  }

  /**
   * Stores a changed custom template. Only ids made by this manager can be saved, so a built-in
   * template cannot be overwritten.
   */
  public void save(Template template) throws TemplateException {
    if (!template.id().startsWith(ID_PREFIX)) {
      throw new TemplateException(Reason.INVALID, "not a custom template: " + template.id());
    }
    TemplateValidator.validate(template);
    store.save(template);
  }

  /** Deletes a custom template. */
  public void delete(String id) throws TemplateException {
    if (!id.startsWith(ID_PREFIX)) {
      throw new TemplateException(Reason.INVALID, "not a custom template: " + id);
    }
    store.delete(id);
  }

  /** The id for a template named {@code name}: its slug after the prefix, numbered if taken. */
  static String uniqueId(String name, Set<String> taken) {
    String base = ID_PREFIX + slug(name);
    if (base.length() > TemplateValidator.MAX_ID_LENGTH - 4) {
      base = base.substring(0, TemplateValidator.MAX_ID_LENGTH - 4);
    }
    String candidate = base;
    for (int n = 2; taken.contains(candidate); n++) {
      candidate = base + "-" + n;
    }
    return candidate;
  }

  private static String slug(String name) {
    StringBuilder out = new StringBuilder();
    boolean pendingDash = false;
    for (char c : name.toLowerCase(Locale.ROOT).toCharArray()) {
      if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
        if (pendingDash && out.length() > 0) {
          out.append('-');
        }
        pendingDash = false;
        out.append(c);
      } else {
        pendingDash = true;
      }
    }
    return out.length() == 0 ? "template" : out.toString();
  }
}
