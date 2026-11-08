package org.codirex.asm.feature.project;

import org.codirex.asm.domain.project.ProjectName;

/**
 * What the project screens ask of the app. The host activity implements it and decides how screens
 * are shown, so this feature never knows about other features or about the navigation mechanism
 * (ADR-0003 rule 2, ADR-0010).
 */
public interface ProjectNavigator {

  /** Show the screen that creates a project. */
  void showCreateProject();

  /** Open a project. */
  void showProject(ProjectName name);

  /** A project was created: leave the create screen. */
  void onProjectCreated();
}
