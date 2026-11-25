package org.codirex.asm.terminal.service;

import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Starts a language server of the userland (ADR-0031). The UI names the language, never a program:
 * only the servers listed here can be run.
 */
final class LanguageServerLauncher {

  private static final String TAG = "LanguageServer";

  private final TerminalEnvironment environment;
  private final File prefix;

  LanguageServerLauncher(File filesDirectory) {
    this.environment = new TerminalEnvironment(filesDirectory);
    this.prefix = org.codirex.asm.terminal.bootstrap.BootstrapInstaller.prefixOf(filesDirectory);
  }

  /** The program of a language's server, or null for a language without one. */
  private static String programOf(String language) {
    if ("java".equals(language)) {
      return "jdtls";
    }
    if ("xml".equals(language)) {
      return "lemminx";
    }
    return null;
  }

  /** The command for a language and a workspace key; null for a language without a server. */
  List<String> command(String language, String workspaceKey) throws IOException {
    String program = programOf(language);
    if (program == null) {
      return null;
    }
    if (!isValidKey(workspaceKey)) {
      throw new IOException("bad workspace key");
    }
    File script = new File(prefix, "bin/" + program);
    if (!script.isFile()) {
      throw new IOException("the language server is not installed: no " + script);
    }
    List<String> command = new ArrayList<>();
    command.add(new File(prefix, "bin/sh").getPath());
    command.add(script.getPath());
    if ("java".equals(language)) {
      // jdt.ls keeps its data per workspace; lemminx keeps none.
      command.add(new File(environment.home(), ".cache/jdtls/workspace-" + workspaceKey).getPath());
    }
    return command;
  }

  /**
   * Starts the server with the environment of the terminal. What it writes to standard error goes
   * to {@code ~/.cache/jdtls/server.log} and to logcat, with the exit code when it ends.
   */
  Process start(String language, String workspaceKey) throws IOException {
    List<String> command = command(language, workspaceKey);
    if (command == null) {
      throw new IOException("no language server for " + language);
    }
    File home = environment.home();
    File logs = new File(home, ".cache/" + programOf(language));
    if (!logs.isDirectory() && !logs.mkdirs()) {
      throw new IOException("cannot create " + logs);
    }
    ProcessBuilder builder = new ProcessBuilder(command);
    Map<String, String> env = builder.environment();
    env.clear();
    env.putAll(environment.variables());
    builder.directory(home);
    Log.i(TAG, "starting " + command);
    Process process = builder.start();
    watchErrors(process, new File(logs, "server.log"));
    return process;
  }

  private static void watchErrors(Process process, File log) {
    Thread thread =
        new Thread(
            () -> {
              try (BufferedReader reader =
                      new BufferedReader(
                          new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
                  FileWriter writer = new FileWriter(log, true)) {
                String line;
                while ((line = reader.readLine()) != null) {
                  Log.w(TAG, line);
                  writer.write(line);
                  writer.write('\n');
                  writer.flush();
                }
              } catch (IOException ignored) {
                // The stream or the log is gone; the exit code below still says how it ended.
              }
              try {
                Log.w(TAG, "the language server ended with code " + process.waitFor());
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            },
            "lsp-stderr");
    thread.setDaemon(true);
    thread.start();
  }

  /** A short name made of letters, digits, dot, dash and underscore. */
  static boolean isValidKey(String key) {
    if (key == null || key.isEmpty() || key.length() > 80 || key.equals(".") || key.equals("..")) {
      return false;
    }
    for (int i = 0; i < key.length(); i++) {
      char c = key.charAt(i);
      boolean ok =
          (c >= 'a' && c <= 'z')
              || (c >= 'A' && c <= 'Z')
              || (c >= '0' && c <= '9')
              || c == '.'
              || c == '-'
              || c == '_';
      if (!ok) {
        return false;
      }
    }
    return true;
  }
}
