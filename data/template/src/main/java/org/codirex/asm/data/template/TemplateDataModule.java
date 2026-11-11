package org.codirex.asm.data.template;

import android.content.Context;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import java.io.File;
import javax.inject.Singleton;
import org.codirex.asm.domain.project.ProjectRepository;
import org.codirex.asm.domain.template.CustomTemplateStore;
import org.codirex.asm.domain.template.LayeredTemplateCatalog;
import org.codirex.asm.domain.template.ProjectCreator;
import org.codirex.asm.domain.template.ProjectScaffold;
import org.codirex.asm.domain.template.TemplateCatalog;
import org.codirex.asm.domain.template.TemplateManager;

/** Hilt wiring for {@code data:template}. Installed automatically in the application graph. */
@Module
@InstallIn(SingletonComponent.class)
abstract class TemplateDataModule {

  @Binds
  abstract CustomTemplateStore bindCustomTemplateStore(FileCustomTemplateStore impl);

  /** Custom templates live in the app's private directory (ADR-0021). */
  @Provides
  @TemplatesRoot
  static File provideTemplatesRoot(@ApplicationContext Context context) {
    return new File(context.getFilesDir(), "templates");
  }

  /** What the IDE offers: the built-in templates, then the custom ones (ADR-0021). */
  @Provides
  @Singleton
  static TemplateCatalog provideTemplateCatalog(
      BuiltInTemplates builtIn, CustomTemplateStore custom) {
    return new LayeredTemplateCatalog(builtIn, custom);
  }

  /** The domain classes have no Hilt annotations (ADR-0003), so they are built here. */
  @Provides
  static TemplateManager provideTemplateManager(
      CustomTemplateStore store, BuiltInTemplates builtIn) {
    return new TemplateManager(store, builtIn);
  }

  @Provides
  static ProjectCreator provideProjectCreator(
      ProjectRepository repository, TemplateCatalog catalog, ProjectScaffold scaffold) {
    return new ProjectCreator(repository, catalog, scaffold);
  }
}
