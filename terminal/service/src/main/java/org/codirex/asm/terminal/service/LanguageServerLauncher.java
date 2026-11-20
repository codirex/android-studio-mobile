package org.codirex.asm.terminal.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Starts a language server of the userland (ADR-0031). The UI names the language, never a program:
 * only the servers listed here can be run.
 */
final class LanguageServerLauncher {

  private final TerminalEnvironment environment;
  private final File prefix;

  LanguageServerLauncher(File filesDirectory) {
    this.environment = new TerminalEnvironment(filesDirectory);
    this.prefix = org.codirex.asm.terminal.bootstrap.BootstrapInstaller.prefixOf(filesDirectory);
  }

  /** The command for a language and a workspace key; null for a language without a server. */
  List<String> command(String language, String workspaceKey) throws IOException {
    if (!"java".equals(language)) {
      return null;
    }
    if (!isValidKey(workspaceKey)) {
      throw new IOException("bad workspace key");
    }
    File script = new File(prefix, "bin/jdtls");
    if (!script.isFile()) {
      throw new IOException("the language server is not installed: no " + script);
    }
    File data = new File(environment.home(), ".cache/jdtls/workspace-" + workspaceKey);
    List<String> command = new ArrayList<>();
    command.add(new File(prefix, "bin/sh").getPath());
    command.add(script.getPath());
    command.add(data.getPath());
    return command;
  }

  /** Starts the server with the environment of the terminal, standard error going to a log. */
  Process start(String language, String workspaceKey) throws IOException {
    List<String> command = command(language, workspaceKey);
    if (command == null) {
      throw new IOException("no language server for " + language);
    }
    File home = environment.home();
    File logs = new File(home, ".cache/jdtls");
    if (!logs.isDirectory() && !logs.mkdirs()) {
      throw new IOException("cannot create " + logs);
    }
    ProcessBuilder builder = new ProcessBuilder(command);
    Map<String, String> env = builder.environment();
    env.clear();
    env.putAll(environment.variables());
    builder.directory(home);
    builder.redirectError(ProcessBuilder.Redirect.appendTo(new File(logs, "server.log")));
    return builder.start();
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
