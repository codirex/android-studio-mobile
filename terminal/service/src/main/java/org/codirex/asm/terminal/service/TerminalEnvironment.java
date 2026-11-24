package org.codirex.asm.terminal.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

  /** Names the person cannot change: the terminal needs them (ADR-0028, as in the domain). */
  private static final List<String> RESERVED =
      Arrays.asList("HOME", "PREFIX", "SHELL", "LD_PRELOAD");

  /** The JDK Gradle builds run on (ADR-0027); it is the JAVA_HOME whenever it is installed. */
  private static final int GRADLE_JDK = 17;

  private static final String BLOCK_BEGIN = "# >>> Android Studio Mobile (managed, do not edit)";
  private static final String BLOCK_END = "# <<< Android Studio Mobile";

  private final File prefix;
  private final File home;
  private final File environmentFile;
  private final File sdkRoot;

  TerminalEnvironment(File filesDirectory) {
    this.prefix = BootstrapInstaller.prefixOf(filesDirectory);
    this.home = BootstrapInstaller.homeOf(filesDirectory);
    this.environmentFile = new File(filesDirectory, "environment.properties");
    this.sdkRoot = new File(filesDirectory, "sdk");
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
    // pkg picks a random Termux mirror and rewrites apt's sources.list unless told not to. The
    // userland trusts only the key of asm-packages, so such a mirror fails with NO_PUBKEY.
    env.put("TERMUX_PKG_NO_MIRROR_SELECT", "1");
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
    // The SDK of the SDK manager (ADR-0030). Gradle and the Android plugin look for it here.
    env.put("ANDROID_HOME", sdkRoot.getPath());
    env.put("ANDROID_SDK_ROOT", sdkRoot.getPath());
    // The aapt2 of Google is built for x86-64 and cannot run here; the one of the userland can.
    // It is handed to Gradle through the gradle.properties of the user home, for every project.
    syncGradleProperties();
    // The person's own variables come last, so they can change what is set above (ADR-0028).
    env.putAll(customVariables());
    return env;
  }

  /**
   * Keeps {@code android.aapt2FromMavenOverride} in {@code ~/.gradle/gradle.properties} (the Gradle
   * user home, so it holds for every project and nobody edits a project file). Only the block
   * between the two markers is ours: the rest of the file is the person's and stays as it is. With
   * no aapt2 installed the block is removed. An environment variable would not do: its name has a
   * dot, and bash drops such variables before Gradle starts. Best effort: a failure is ignored.
   */
  void syncGradleProperties() {
    File aapt2 = new File(prefix, "bin/aapt2");
    File file = new File(home, ".gradle/gradle.properties");
    try {
      String current =
          file.isFile() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : "";
      String rest = withoutManagedBlock(current);
      String next = rest;
      if (aapt2.isFile()) {
        if (!rest.isEmpty() && !rest.endsWith("\n")) {
          rest += "\n";
        }
        next =
            rest
                + BLOCK_BEGIN
                + "\nandroid.aapt2FromMavenOverride="
                + aapt2.getPath()
                + "\n"
                + BLOCK_END
                + "\n";
      }
      if (next.equals(current)) {
        return;
      }
      if (next.isEmpty()) {
        file.delete();
        return;
      }
      File parent = file.getParentFile();
      if (!parent.isDirectory() && !parent.mkdirs()) {
        return;
      }
      Files.write(file.toPath(), next.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      // The build then fails with Gradle's own message; the terminal still starts.
    }
  }

  /** The text without the lines from the begin marker to the end marker, both included. */
  static String withoutManagedBlock(String text) {
    StringBuilder kept = new StringBuilder();
    boolean inside = false;
    for (String line : text.split("\n", -1)) {
      if (line.equals(BLOCK_BEGIN)) {
        inside = true;
      } else if (inside && line.equals(BLOCK_END)) {
        inside = false;
      } else if (!inside) {
        kept.append(line).append('\n');
      }
    }
    // split leaves a last empty piece after a final newline; drop the newline it added.
    if (kept.length() > 0) {
      kept.setLength(kept.length() - 1);
    }
    return kept.toString();
  }

  /**
   * The variables the person set, read from {@code files/environment.properties} (written by {@code
   * data:environment}, same format). Entries with an invalid or reserved name are ignored; an
   * unreadable file means none.
   */
  Map<String, String> customVariables() {
    Map<String, String> custom = new LinkedHashMap<>();
    if (!environmentFile.isFile()) {
      return custom;
    }
    Properties properties = new Properties();
    try (Reader reader =
        new InputStreamReader(new FileInputStream(environmentFile), StandardCharsets.UTF_8)) {
      properties.load(reader);
    } catch (IOException e) {
      return custom;
    }
    for (String name : properties.stringPropertyNames()) {
      if (isValidName(name) && !RESERVED.contains(name)) {
        custom.put(name, properties.getProperty(name));
      }
    }
    return custom;
  }

  /** Letters, digits and underscores, not starting with a digit. */
  static boolean isValidName(String name) {
    if (name.isEmpty()) {
      return false;
    }
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      boolean letter = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
      boolean digit = c >= '0' && c <= '9';
      if (!letter && !(digit && i > 0)) {
        return false;
      }
    }
    return true;
  }

  /**
   * The JDK of the shell: the Gradle JDK (17) when it is installed, so that installing a newer JDK
   * (the language server needs 25) does not move Gradle builds to it; otherwise the newest JDK of
   * the userland. A JDK is a folder {@code lib/jvm/java-NN-...} with a {@code bin/java}. {@code
   * null} while no JDK is installed (ADR-0027).
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
      if (!new File(folder, "bin/java").isFile()) {
        continue;
      }
      if (version == GRADLE_JDK) {
        return folder;
      }
      if (version > bestVersion) {
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
