package org.codirex.asm.terminal.pty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A process running on a pseudo-terminal (ADR-0023): the shell or tool the terminal shows. It is
 * a session of its own, so signals sent with {@link #signal} reach the whole process group, a
 * shell and what it started.
 *
 * <p>{@link #read} and {@link #waitFor} block and belong on their own threads or jobs; {@link
 * #write}, {@link #resize}, {@link #signal} and {@link #close} return quickly. The descriptor is
 * closed by {@link #close}, but never while another thread is inside {@link #read} or {@link
 * #write}: the last of them closes it, so a descriptor number is never reused under a reader.
 */
public final class PtyProcess {

  public static final int SIGHUP = 1;
  public static final int SIGINT = 2;
  public static final int SIGKILL = 9;
  public static final int SIGTERM = 15;

  private static final int MAX_SIZE = 65_535;

  private final int master;
  private final int pid;

  private final Object lock = new Object();
  private int active;
  private boolean closeRequested;
  private boolean masterClosed;

  private final Object waitLock = new Object();
  private Integer exitCode;

  private PtyProcess(int master, int pid) {
    this.master = master;
    this.pid = pid;
  }

  /** Whether the native library is there; false on a JVM that does not have it (unit tests). */
  public static boolean isSupported() {
    return PtyNative.isAvailable();
  }

  /**
   * Starts {@code path} on a new terminal.
   *
   * @param argv the arguments, the first one being the program's own name
   * @param environment the whole environment of the process; nothing is inherited
   * @param directory the working directory, or null to keep the current one
   * @throws IOException if the program cannot be started, for example it does not exist
   */
  public static PtyProcess start(
      String path,
      List<String> argv,
      Map<String, String> environment,
      File directory,
      int rows,
      int columns)
      throws IOException {
    if (!PtyNative.isAvailable()) {
      throw new IOException("the terminal library is not available");
    }
    Objects.requireNonNull(argv, "argv");
    if (Objects.requireNonNull(path, "path").isEmpty()) {
      throw new IllegalArgumentException("path must not be empty");
    }
    checkSize(rows, columns);
    List<String> env = new ArrayList<>(environment.size());
    for (Map.Entry<String, String> entry : environment.entrySet()) {
      env.add(entry.getKey() + "=" + entry.getValue());
    }
    int[] started =
        PtyNative.spawn(
            path,
            argv.toArray(new String[0]),
            env.toArray(new String[0]),
            directory == null ? null : directory.getPath(),
            rows,
            columns);
    return new PtyProcess(started[0], started[1]);
  }

  /** The process id, which is also the id of its process group. */
  public int pid() {
    return pid;
  }

  /**
   * Reads what the process printed.
   *
   * @return the number of bytes, or -1 when the process has ended and everything was read, or
   *     the process was closed
   */
  public int read(byte[] buffer, int offset, int length) throws IOException {
    checkRange(buffer, offset, length);
    if (!enter()) {
      return -1;
    }
    try {
      return PtyNative.read(master, buffer, offset, length);
    } finally {
      leave();
    }
  }

  /** Sends bytes to the process as if they were typed. */
  public void write(byte[] buffer, int offset, int length) throws IOException {
    checkRange(buffer, offset, length);
    if (!enter()) {
      throw new IOException("the terminal is closed");
    }
    try {
      PtyNative.write(master, buffer, offset, length);
    } finally {
      leave();
    }
  }

  /** Tells the process that the window has a new size. */
  public void resize(int rows, int columns) throws IOException {
    checkSize(rows, columns);
    if (!enter()) {
      throw new IOException("the terminal is closed");
    }
    try {
      PtyNative.setSize(master, rows, columns);
    } finally {
      leave();
    }
  }

  /** Sends a signal to the whole process group. A group that is gone is not an error. */
  public void signal(int signal) throws IOException {
    PtyNative.signal(pid, signal);
  }

  /**
   * Waits for the process to end and returns its exit code, or minus the number of the signal that
   * ended it. The result is kept, so this can be called again and from several threads.
   */
  public int waitFor() {
    synchronized (waitLock) {
      if (exitCode == null) {
        exitCode = PtyNative.waitFor(pid);
      }
      return exitCode;
    }
  }

  /**
   * Ends the process (hangup, as when a terminal window closes, then a kill for what stays) and
   * releases the terminal. Safe to call more than once.
   */
  public void close() {
    try {
      PtyNative.signal(pid, SIGHUP);
      PtyNative.signal(pid, SIGKILL);
    } catch (IOException ignored) {
      // Nothing more can be done for a process that cannot be signalled.
    }
    synchronized (lock) {
      closeRequested = true;
      if (active == 0) {
        closeMaster();
      }
    }
  }

  private boolean enter() {
    synchronized (lock) {
      if (closeRequested) {
        return false;
      }
      active++;
      return true;
    }
  }

  private void leave() {
    synchronized (lock) {
      active--;
      if (closeRequested && active == 0) {
        closeMaster();
      }
    }
  }

  private void closeMaster() {
    if (!masterClosed) {
      masterClosed = true;
      PtyNative.close(master);
    }
  }

  private static void checkSize(int rows, int columns) {
    if (rows < 1 || rows > MAX_SIZE || columns < 1 || columns > MAX_SIZE) {
      throw new IllegalArgumentException("bad terminal size " + rows + "x" + columns);
    }
  }

  private static void checkRange(byte[] buffer, int offset, int length) {
    if (offset < 0 || length < 0 || offset > buffer.length - length) {
      throw new IndexOutOfBoundsException("offset " + offset + ", length " + length);
    }
  }
}
