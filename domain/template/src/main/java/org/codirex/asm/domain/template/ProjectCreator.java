package org.codirex.asm.domain.template;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.codirex.asm.domain.project.Project;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectException.Reason;
import org.codirex.asm.domain.project.ProjectRepository;
import org.codirex.asm.domain.project.ProjectSpec;

/**
 * Creates a project together with the first files of its type. It renders the default template of
 * the type, creates the project and writes the files. A failed creation leaves nothing behind: a
 * template that cannot be rendered stops before the project exists, and a project whose files
 * cannot be written is deleted again.
 *
 * <p><b>{@link #create} blocks.</b> Callers run it in a background job.
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

  /**
   * Creates the project. A type without a template gives an empty project.
   *
   * @throws ProjectException with reason {@code ALREADY_EXISTS} if the name is taken, or {@code
   *     STORAGE} if the storage failed or the template could not be rendered or written
   */
  public Project create(ProjectSpec spec) throws ProjectException {
    List<TemplateFile> files = renderDefaultTemplate(spec);
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

  private List<TemplateFile> renderDefaultTemplate(ProjectSpec spec) throws ProjectException {
    List<Template> templates = catalog.templatesFor(spec.type());
    if (templates.isEmpty()) {
      return Collections.emptyList();
    }
    try {
      return templates.get(0).render(spec);
    } catch (TemplateException e) {
      throw new ProjectException(Reason.STORAGE, "cannot render template " + templates.get(0).id(), e);
    }
  }
}
