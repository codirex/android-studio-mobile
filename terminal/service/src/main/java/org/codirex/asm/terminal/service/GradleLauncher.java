package org.codirex.asm.terminal.service;

import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Starts a Gradle build of a project of the app (ADR-0032). The UI names a task, never a program:
 * the program is the project's own wrapper (run through {@code sh}, so it needs no execute bit) or
 * the {@code gradle} of the userland.
 */
final class GradleLauncher {

  private static final String TAG = "GradleLauncher";

  private final TerminalEnvironment environment;
  private final File prefix;
  private final File projects;

  GradleLauncher(File filesDirectory) {
    this.environment = new TerminalEnvironment(filesDirectory);
    this.prefix = org.codirex.asm.terminal.bootstrap.BootstrapInstaller.prefixOf(filesDirectory);
    this.projects = new File(filesDirectory, "projects");
  }

  /**
   * The command for a task in a project folder.
   *
   * @throws IllegalArgumentException for a task name that is not allowed or a folder that is not a
   *     project folder of the app
   * @throws IOException if there is no way to run Gradle
   */
  List<String> command(String projectDirectory, String task) throws IOException {
    if (!isValidTask(task)) {
      throw new IllegalArgumentException("not a task name: " + task);
    }
    File directory = projectFolder(projectDirectory);
    List<String> command = new ArrayList<>();
    command.add(new File(prefix, "bin/sh").getPath());
    if (new File(directory, "gradlew").isFile()) {
      command.add("./gradlew");
    } else {
      File gradle = new File(prefix, "bin/gradle");
      if (!gradle.isFile()) {
        throw new IOException("gradle is not installed: no " + gradle);
      }
      command.add(gradle.getPath());
    }
    command.add("--console=plain");
    command.add(task);
    return command;
  }

  /** Starts the build with the environment of the terminal; standard error joins the output. */
  Process start(String projectDirectory, String task) throws IOException {
    List<String> command = command(projectDirectory, task);
    File directory = projectFolder(projectDirectory);
    ProcessBuilder builder = new ProcessBuilder(command);
    Map<String, String> env = builder.environment();
    env.clear();
    env.putAll(environment.variables());
    builder.directory(directory);
    builder.redirectErrorStream(true);
    Log.i(TAG, "running " + command + " in " + directory);
    return builder.start();
  }

  /** The folder, if it is a direct child of the projects folder of the app. */
  private File projectFolder(String path) throws IOException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("no project folder");
    }
    File directory = new File(path).getCanonicalFile();
    File parent = directory.getParentFile();
    if (!directory.isDirectory()
        || parent == null
        || !parent.equals(projects.getCanonicalFile())) {
      throw new IllegalArgumentException("not a project folder: " + path);
    }
    return directory;
  }

  /** Letters, digits and {@code _ : . -}, not starting with a dash (as {@code GradleTask}). */
  static boolean isValidTask(String name) {
    if (name == null || name.isEmpty() || name.length() > 200 || name.charAt(0) == '-') {
      return false;
    }
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      boolean ok =
          (c >= 'a' && c <= 'z')
              || (c >= 'A' && c <= 'Z')
              || (c >= '0' && c <= '9')
              || c == '_'
              || c == ':'
              || c == '.'
              || c == '-';
      if (!ok) {
        return false;
      }
    }
    return true;
  }
}
