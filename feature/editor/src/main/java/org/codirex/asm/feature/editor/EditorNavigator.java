package org.codirex.asm.feature.editor;

/**
 * What the editor needs from the app. The host activity implements it, so this feature never knows
 * the file tree or any other feature (ADR-0010).
 */
public interface EditorNavigator {

  /** The user asked to see the project's files. */
  void showFileTree();

  /**
   * The user asked to install a package of the userland (the Java language server, ADR-0031). The
   * host runs the package manager where the person can see it.
   */
  void installPackage(String packageName);

  /** The user asked to see a Gradle task run on the build screen (the details of a failed sync). */
  void showBuild(String task);
}
