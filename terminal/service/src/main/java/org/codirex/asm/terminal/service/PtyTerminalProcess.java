package org.codirex.asm.terminal.service;

import java.io.IOException;
import org.codirex.asm.terminal.pty.PtyProcess;

/** A {@link TerminalProcess} on a real pseudo-terminal. */
final class PtyTerminalProcess implements TerminalProcess {

  private final PtyProcess process;

  PtyTerminalProcess(PtyProcess process) {
    this.process = process;
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    return process.read(buffer, offset, length);
  }

  @Override
  public void write(byte[] buffer, int offset, int length) throws IOException {
    process.write(buffer, offset, length);
  }

  @Override
  public void resize(int rows, int columns) throws IOException {
    process.resize(rows, columns);
  }

  @Override
  public void signal(int signal) throws IOException {
    process.signal(signal);
  }

  @Override
  public int waitFor() {
    return process.waitFor();
  }

  @Override
  public void close() {
    process.close();
  }
}
