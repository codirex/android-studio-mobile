package org.codirex.asm.terminal.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.codirex.asm.terminal.bootstrap.BootstrapInstaller;

/**
 * What a shell of the terminal sees: the program, its arguments and its whole environment, derived
 * from the app's files directory (ADR-0024). Nothing is inherited from the service process.
 */
final class TerminalEnvironment {

  /** The libraries of termux-exec that make programs find their interpreters, newest first. */
  private static final String[] PRELOAD = {
    "lib/libtermux-exec-ld-preload.so", "lib/libtermux-exec.so"
  };

  private final File prefix;
  private final File home;

  TerminalEnvironment(File filesDirectory) {
    this.prefix = BootstrapInstaller.prefixOf(filesDirectory);
    this.home = BootstrapInstaller.homeOf(filesDirectory);
  }

  File home() {
    return home;
  }

  /** The login shell. */
  File shell() {
    return new File(prefix, "bin/bash");
  }

  /** The arguments of the shell: a leading dash in its name makes it a login shell. */
  List<String> arguments() {
    return new ArrayList<>(Arrays.asList("-bash"));
  }

  /** The whole environment. */
  Map<String, String> variables() {
    Map<String, String> env = new LinkedHashMap<>();
    env.put("HOME", home.getPath());
    env.put("PREFIX", prefix.getPath());
    env.put("PATH", new File(prefix, "bin").getPath());
    env.put("TMPDIR", new File(prefix, "tmp").getPath());
    env.put("SHELL", shell().getPath());
    env.put("TERM", "xterm-256color");
    env.put("COLORTERM", "truecolor");
    env.put("LANG", "en_US.UTF-8");
    env.put("ANDROID_ROOT", "/system");
    env.put("ANDROID_DATA", "/data");
    env.put("EXTERNAL_STORAGE", "/sdcard");
    for (String library : PRELOAD) {
      File file = new File(prefix, library);
      if (file.isFile()) {
        env.put("LD_PRELOAD", file.getPath());
        break;
      }
    }
    File javaHome = javaHome();
    if (javaHome != null) {
      env.put("JAVA_HOME", javaHome.getPath());
    }
    return env;
  }

  /**
   * The newest JDK of the userland: a folder {@code lib/jvm/java-NN-...} with a {@code bin/java}.
   * {@code null} while no JDK is installed (ADR-0027).
   */
  File javaHome() {
    File[] folders = new File(prefix, "lib/jvm").listFiles();
    if (folders == null) {
      return null;
    }
    File best = null;
    int bestVersion = -1;
    for (File folder : folders) {
      int version = majorVersionOf(folder.getName());
      if (version > bestVersion && new File(folder, "bin/java").isFile()) {
        best = folder;
        bestVersion = version;
      }
    }
    return best;
  }

  /** 17 for {@code java-17-openjdk}; -1 for a name that is not a JDK folder. */
  static int majorVersionOf(String folderName) {
    if (!folderName.startsWith("java-")) {
      return -1;
    }
    int end = folderName.indexOf('-', 5);
    String digits = end < 0 ? folderName.substring(5) : folderName.substring(5, end);
    try {
      return Integer.parseInt(digits);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /** The directory a session starts in: the asked one if it exists, else the home directory. */
  File startDirectory(String asked) {
    if (asked != null && !asked.isEmpty()) {
      File directory = new File(asked);
      if (directory.isDirectory()) {
        return directory;
      }
    }
    return home;
  }
}
