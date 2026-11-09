package org.codirex.asm.data.template;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import org.codirex.asm.domain.project.ProjectRepository;
import org.codirex.asm.domain.template.ProjectCreator;
import org.codirex.asm.domain.template.ProjectScaffold;
import org.codirex.asm.domain.template.TemplateCatalog;

/** Hilt wiring for {@code data:template}. Installed automatically in the application graph. */
@Module
@InstallIn(SingletonComponent.class)
abstract class TemplateDataModule {

  @Binds
  abstract TemplateCatalog bindTemplateCatalog(BuiltInTemplates impl);

  /** The domain class has no Hilt annotations (ADR-0003), so it is built here. */
  @Provides
  static ProjectCreator provideProjectCreator(
      ProjectRepository repository, TemplateCatalog catalog, ProjectScaffold scaffold) {
    return new ProjectCreator(repository, catalog, scaffold);
  }
}
