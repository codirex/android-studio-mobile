package org.codirex.asm.terminal.service;

import java.io.File;
import java.io.IOException;
import org.codirex.asm.terminal.pty.PtyProcess;

/** Starts the login shell of the userland on a pseudo-terminal. */
final class PtyLauncher implements SessionManager.Launcher {

  private final TerminalEnvironment environment;

  PtyLauncher(File filesDirectory) {
    this.environment = new TerminalEnvironment(filesDirectory);
  }

  @Override
  public TerminalProcess start(String directory, int rows, int columns) throws IOException {
    File shell = environment.shell();
    if (!shell.canExecute()) {
      throw new IOException("the userland is not installed: no " + shell);
    }
    File home = environment.home();
    if (!home.isDirectory() && !home.mkdirs()) {
      throw new IOException("cannot create " + home);
    }
    PtyProcess process =
        PtyProcess.start(
            shell.getPath(),
            environment.arguments(),
            environment.variables(),
            environment.startDirectory(directory),
            rows,
            columns);
    return new PtyTerminalProcess(process);
  }
}
