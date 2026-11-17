package org.codirex.asm.feature.toolchain;

/**
 * What the toolchain screen asks of the app. The host activity implements it, so this feature never
 * knows about the terminal (ADR-0003 rule 2, ADR-0010).
 */
public interface ToolchainNavigator {

  /**
   * Installs a package of the userland, for example {@code openjdk-17}. The app decides how: today
   * it opens a terminal that runs the package manager, so the person sees what happens.
   */
  void installPackage(String packageName);
}
