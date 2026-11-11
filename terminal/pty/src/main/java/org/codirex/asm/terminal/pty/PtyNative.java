package org.codirex.asm.terminal.pty;

import java.io.IOException;

/**
 * The native calls behind {@link PtyProcess} (ADR-0023). Every method may block except {@link
 * #setSize}, {@link #signal} and {@link #close}. Use {@link PtyProcess}, not this class.
 */
final class PtyNative {

  private static final boolean AVAILABLE = load();

  private PtyNative() {}

  /** Whether the native library could be loaded; false on a plain JVM without it. */
  static boolean isAvailable() {
    return AVAILABLE;
  }

  private static boolean load() {
    try {
      System.loadLibrary("asm_pty");
      return true;
    } catch (UnsatisfiedLinkError e) {
      return false;
    }
  }

  /** Starts the process on a new terminal. Returns {master descriptor, process id}. */
  static native int[] spawn(
      String path, String[] argv, String[] envp, String cwd, int rows, int columns)
      throws IOException;

  static native void setSize(int master, int rows, int columns) throws IOException;

  /** Waits for the process: its exit code, or minus the signal that ended it. */
  static native int waitFor(int pid);

  static native void signal(int pid, int signal) throws IOException;

  /** Reads into {@code buffer}; returns the number of bytes, or -1 at the end of the output. */
  static native int read(int master, byte[] buffer, int offset, int length) throws IOException;

  static native void write(int master, byte[] buffer, int offset, int length) throws IOException;

  static native void close(int master);
}
