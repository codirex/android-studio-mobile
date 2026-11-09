package org.codirex.asm.domain.template;

import java.util.List;
import org.codirex.asm.domain.project.ProjectType;

/**
 * The templates the IDE can offer. The domain defines it; a data module implements it.
 *
 * <p><b>Every method may block</b> (a catalog may read files). Callers run it in a background job.
 */
public interface TemplateCatalog {

  /** The templates that create a project of {@code type}; the first one is the default. */
  List<Template> templatesFor(ProjectType type);
}
