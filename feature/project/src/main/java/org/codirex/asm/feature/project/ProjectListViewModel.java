package org.codirex.asm.feature.project;

import androidx.lifecycle.LiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import javax.inject.Inject;
import org.codirex.asm.core.concurrency.RealmFactory;
import org.codirex.asm.core.concurrency.RealmViewModel;
import org.codirex.asm.core.concurrency.UiStateHolder;
import org.codirex.asm.domain.project.Project;
import org.codirex.asm.domain.project.ProjectRepository;

/** Loads and deletes projects off the main thread. All methods are called on the main thread. */
@HiltViewModel
final class ProjectListViewModel extends RealmViewModel {

  private final ProjectRepository repository;
  private final UiStateHolder<ProjectListState> state =
      new UiStateHolder<>(ProjectListState.initial());

  /** Bumped by every refresh; a result from an older refresh is ignored. */
  private int generation;

  @Inject
  ProjectListViewModel(RealmFactory factory, ProjectRepository repository) {
    super(factory);
    this.repository = repository;
  }

  LiveData<ProjectListState> state() {
    return state.observable();
  }

  /** Reloads the list. */
  void refresh() {
    final int mine = ++generation;
    state.set(state.get().withLoading());
    this.<List<Project>>runAsync(
        checkpoint -> repository.list(),
        outcome -> {
          if (mine != generation) {
            return;
          }
          if (outcome.isSuccess()) {
            state.set(state.get().loaded(outcome.valueOrNull()));
          } else if (outcome.isFailure()) {
            state.set(state.get().failed(Failures.reasonOf(outcome.failureOrNull())));
          }
        });
  }

  /** Deletes a project, then reloads the list whatever happened. */
  void delete(Project project) {
    this.<Boolean>runAsync(
        checkpoint -> {
          repository.delete(project.name());
          return Boolean.TRUE;
        },
        outcome -> {
          if (outcome.isFailure()) {
            state.set(state.get().failed(Failures.reasonOf(outcome.failureOrNull())));
          }
          refresh();
        });
  }

  /** The screen showed the error; do not show it again. */
  void errorHandled() {
    state.set(state.get().errorHandled());
  }
}
