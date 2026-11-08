package org.codirex.asm.feature.project;

import androidx.lifecycle.LiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import org.codirex.asm.core.concurrency.RealmFactory;
import org.codirex.asm.core.concurrency.RealmViewModel;
import org.codirex.asm.core.concurrency.UiStateHolder;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.Project;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectRepository;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;

/** Creates a project off the main thread. All methods are called on the main thread. */
@HiltViewModel
final class CreateProjectViewModel extends RealmViewModel {

  private final ProjectRepository repository;
  private final UiStateHolder<CreateProjectState> state =
      new UiStateHolder<>(CreateProjectState.idle());

  @Inject
  CreateProjectViewModel(RealmFactory factory, ProjectRepository repository) {
    super(factory);
    this.repository = repository;
  }

  LiveData<CreateProjectState> state() {
    return state.observable();
  }

  /**
   * Creates the project. Input that is not valid is ignored: the screen keeps the button disabled
   * until both fields validate, this is the second line of defence.
   */
  void create(String name, String packageName, ProjectType type) {
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
        checkpoint -> repository.create(spec),
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
