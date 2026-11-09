package org.codirex.asm.data.project;

import android.content.Context;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import java.io.File;
import org.codirex.asm.domain.editor.ProjectDocuments;
import org.codirex.asm.domain.filetree.ProjectFiles;
import org.codirex.asm.domain.project.ProjectRepository;
import org.codirex.asm.domain.template.ProjectScaffold;

/** Hilt wiring for {@code data:project}. Installed automatically in the application graph. */
@Module
@InstallIn(SingletonComponent.class)
abstract class ProjectDataModule {

  @Binds
  abstract ProjectRepository bindProjectRepository(FileProjectRepository impl);

  @Binds
  abstract ProjectFiles bindProjectFiles(FileProjectFiles impl);

  @Binds
  abstract ProjectDocuments bindProjectDocuments(FileProjectDocuments impl);

  @Binds
  abstract ProjectScaffold bindProjectScaffold(FileProjectScaffold impl);

  /** Projects live in the app's private directory (ADR-0012). */
  @Provides
  @ProjectsRoot
  static File provideProjectsRoot(@ApplicationContext Context context) {
    return new File(context.getFilesDir(), "projects");
  }
}
