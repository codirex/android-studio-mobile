package org.codirex.asm.domain.template;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.codirex.asm.domain.project.Project;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectException.Reason;
import org.codirex.asm.domain.project.ProjectRepository;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;

/**
 * Creates a project together with the first files of its type. It renders a template of the type
 * (the chosen one, or the default), creates the project and writes the files. A failed creation
 * leaves nothing behind: a template that cannot be rendered stops before the project exists, and a
 * project whose files cannot be written is deleted again.
 *
 * <p><b>{@link #create} and {@link #templatesFor} block.</b> Callers run them in a background job.
 */
public final class ProjectCreator {

  private final ProjectRepository repository;
  private final TemplateCatalog catalog;
  private final ProjectScaffold scaffold;

  public ProjectCreator(
      ProjectRepository repository, TemplateCatalog catalog, ProjectScaffold scaffold) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.scaffold = Objects.requireNonNull(scaffold, "scaffold");
  }

  /** The templates a project of {@code type} can start from; the first one is the default. */
  public List<Template> templatesFor(ProjectType type) {
    return catalog.templatesFor(type);
  }

  /** Creates the project from the default template of its type. See {@link #create(ProjectSpec, String)}. */
  public Project create(ProjectSpec spec) throws ProjectException {
    return create(spec, null);
  }

  /**
   * Creates the project from the template with the given id. {@code null} means the default
   * template; a type without a template then gives an empty project.
   *
   * @throws ProjectException with reason {@code ALREADY_EXISTS} if the name is taken, or {@code
   *     STORAGE} if the storage failed, the template does not exist for the type, or it could not
   *     be rendered or written
   */
  public Project create(ProjectSpec spec, String templateId) throws ProjectException {
    List<TemplateFile> files = render(spec, templateId);
    Project project = repository.create(spec);
    if (files.isEmpty()) {
      return project;
    }
    try {
      scaffold.write(spec.name(), files);
    } catch (TemplateException e) {
      try {
        repository.delete(spec.name());
      } catch (ProjectException ignored) {
        // Nothing more can be done; report the original failure.
      }
      throw new ProjectException(Reason.STORAGE, "cannot write the template files", e);
    }
    return project;
  }

  private List<TemplateFile> render(ProjectSpec spec, String templateId) throws ProjectException {
    List<Template> templates = catalog.templatesFor(spec.type());
    Template chosen = null;
    if (templateId == null) {
      if (templates.isEmpty()) {
        return Collections.emptyList();
      }
      chosen = templates.get(0);
    } else {
      for (Template template : templates) {
        if (template.id().equals(templateId)) {
          chosen = template;
          break;
        }
      }
      if (chosen == null) {
        throw new ProjectException(
            Reason.STORAGE, "no template " + templateId + " for " + spec.type());
      }
    }
    try {
      return chosen.render(spec);
    } catch (TemplateException e) {
      throw new ProjectException(Reason.STORAGE, "cannot render template " + chosen.id(), e);
    }
  }
}
