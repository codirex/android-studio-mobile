package org.codirex.asm.feature.build;

/** What the build screen needs from the app (ADR-0010). The activity implements it. */
public interface BuildNavigator {

  /**
   * Shows a file the compiler complained about, in the editor of the project it belongs to.
   *
   * @param absolutePath the path as the compiler printed it
   * @param line the 1-based line of the problem
   */
  void openBuildFile(String absolutePath, int line);
}
