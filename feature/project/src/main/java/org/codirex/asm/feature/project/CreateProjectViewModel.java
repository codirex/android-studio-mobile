package org.codirex.asm.feature.project;

import androidx.lifecycle.LiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import javax.inject.Inject;
import org.codirex.asm.core.concurrency.RealmFactory;
import org.codirex.asm.core.concurrency.RealmViewModel;
import org.codirex.asm.core.concurrency.UiStateHolder;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.Project;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.domain.template.ProjectCreator;
import org.codirex.asm.domain.template.Template;

/**
 * Creates a project, with the first files of its type, off the main thread, and lists the templates
 * it can start from. All methods are called on the main thread.
 */
@HiltViewModel
final class CreateProjectViewModel extends RealmViewModel {

  private final ProjectCreator creator;
  private final UiStateHolder<CreateProjectState> state =
      new UiStateHolder<>(CreateProjectState.idle());
  private final UiStateHolder<TemplateChoices> choices =
      new UiStateHolder<>(TemplateChoices.none());

  @Inject
  CreateProjectViewModel(RealmFactory factory, ProjectCreator creator) {
    super(factory);
    this.creator = creator;
  }

  LiveData<CreateProjectState> state() {
    return state.observable();
  }

  LiveData<TemplateChoices> templates() {
    return choices.observable();
  }

  /**
   * Loads the templates of a type. Asking again for the type already loaded, or loading, does
   * nothing. An answer for a type that is no longer the asked one is dropped.
   */
  void showTemplates(ProjectType type) {
    if (choices.get().type() == type) {
      return;
    }
    choices.set(TemplateChoices.loading(type));
    this.<List<Template>>runAsync(
        checkpoint -> creator.templatesFor(type),
        outcome -> {
          if (choices.get().type() != type) {
            return;
          }
          if (outcome.isSuccess()) {
            choices.set(TemplateChoices.of(type, outcome.valueOrNull()));
          }
          // On failure the choice stays hidden and the default template is used.
        });
  }

  /**
   * Creates the project from the template with {@code templateId}, or from the default template
   * when it is {@code null}. Input that is not valid is ignored: the screen keeps the button
   * disabled until both fields validate, this is the second line of defence.
   */
  void create(String name, String packageName, ProjectType type, String templateId) {
    if (state.get().busy()) {
      return;
    }
    if (!ProjectName.validate(name).isValid() || !PackageName.validate(packageName).isValid()) {
      return;
    }
    final ProjectSpec spec =
        new ProjectSpec(ProjectName.of(name), PackageName.of(packageName), type);
    state.set(CreateProjectState.inProgress());
    this.<Project>runAsync(
        checkpoint -> creator.create(spec, templateId),
        outcome -> {
          if (outcome.isSuccess()) {
            state.set(CreateProjectState.created());
          } else if (outcome.isFailure()) {
            state.set(CreateProjectState.failed(Failures.reasonOf(outcome.failureOrNull())));
          } else {
            state.set(CreateProjectState.idle());
          }
        });
  }

  /** The screen reacted to the created or failed event; do not repeat it. */
  void eventHandled() {
    if (!state.get().busy()) {
      state.set(CreateProjectState.idle());
    }
  }
}
