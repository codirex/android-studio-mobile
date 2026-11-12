package org.codirex.asm.terminal.service;

import java.io.IOException;

/** What a session needs from the program it runs; {@code PtyProcess} in production. */
interface TerminalProcess {

  /** Blocks. Returns the number of bytes, or -1 at the end of the output. */
  int read(byte[] buffer, int offset, int length) throws IOException;

  void write(byte[] buffer, int offset, int length) throws IOException;

  void resize(int rows, int columns) throws IOException;

  void signal(int signal) throws IOException;

  /** Blocks until the program ends; its exit code, or minus the signal that ended it. */
  int waitFor();

  /** Ends the program and releases the terminal. Safe to call more than once. */
  void close();
}
