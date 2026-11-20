package org.codirex.asm.terminal.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Joins a connection (a socket to the UI process) with the standard streams of a program, byte for
 * byte, for as long as both live (ADR-0031). When the connection ends the program is stopped; when
 * the program ends the connection is closed.
 */
final class StdioBridge {

  private static final int BUFFER = 16 * 1024;

  private StdioBridge() {}

  /**
   * Starts the two copying threads and returns at once.
   *
   * @param connectionIn what the UI sends
   * @param connectionOut what the program says
   * @param connection closed when the program has ended (this must unblock a read of {@code
   *     connectionIn})
   */
  static void start(
      InputStream connectionIn,
      OutputStream connectionOut,
      Closeable connection,
      Process process) {
    Thread toProgram =
        new Thread(
            () -> {
              copy(connectionIn, process.getOutputStream());
              // The UI is gone: end the program, which ends the other thread too.
              process.destroy();
            },
            "lsp-to-program");
    Thread fromProgram =
        new Thread(
            () -> {
              copy(process.getInputStream(), connectionOut);
              process.destroy();
              try {
                connection.close();
              } catch (IOException ignored) {
                // Nothing more to do with it.
              }
            },
            "lsp-from-program");
    toProgram.setDaemon(true);
    fromProgram.setDaemon(true);
    toProgram.start();
    fromProgram.start();
  }

  /** Copies until the end of {@code in} or a failure of either side; then closes {@code out}. */
  private static void copy(InputStream in, OutputStream out) {
    byte[] bytes = new byte[BUFFER];
    try {
      int count;
      while ((count = in.read(bytes)) >= 0) {
        if (count > 0) {
          out.write(bytes, 0, count);
          out.flush();
        }
      }
    } catch (IOException ignored) {
      // One side is gone; the caller ends the other.
    } finally {
      try {
        out.close();
      } catch (IOException ignored) {
        // Already closed.
      }
    }
  }
}
