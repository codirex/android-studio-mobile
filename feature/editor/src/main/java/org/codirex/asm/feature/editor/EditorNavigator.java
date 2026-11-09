package org.codirex.asm.feature.editor;

/**
 * What the editor needs from the app. The host activity implements it, so this feature never knows
 * the file tree or any other feature (ADR-0010).
 */
public interface EditorNavigator {

  /** The user asked to see the project's files. */
  void showFileTree();
}
