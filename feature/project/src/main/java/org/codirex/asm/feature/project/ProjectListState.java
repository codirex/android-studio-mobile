package org.codirex.asm.feature.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codirex.asm.domain.project.Project;
import org.codirex.asm.domain.project.ProjectException.Reason;

/**
 * Everything the project list screen shows. Immutable.
 *
 * <p>{@link #error()} is a one-time message: the screen shows it and then tells the view model it
 * was handled, so a rotation does not show it twice.
 */
final class ProjectListState {

  private final List<Project> projects;
  private final boolean loading;
  private final Reason error;

  private ProjectListState(List<Project> projects, boolean loading, Reason error) {
    this.projects = Collections.unmodifiableList(new ArrayList<>(projects));
    this.loading = loading;
    this.error = error;
  }

  static ProjectListState initial() {
    return new ProjectListState(Collections.emptyList(), true, null);
  }

  ProjectListState withLoading() {
    return new ProjectListState(projects, true, error);
  }

  ProjectListState loaded(List<Project> newProjects) {
    return new ProjectListState(newProjects, false, error);
  }

  ProjectListState failed(Reason reason) {
    return new ProjectListState(projects, false, reason);
  }

  ProjectListState errorHandled() {
    return new ProjectListState(projects, loading, null);
  }

  List<Project> projects() {
    return projects;
  }

  boolean isLoading() {
    return loading;
  }

  /** A failure not yet shown to the user, or null. */
  Reason error() {
    return error;
  }
}
