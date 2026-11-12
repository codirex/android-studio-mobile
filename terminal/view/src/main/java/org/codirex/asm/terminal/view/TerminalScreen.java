package org.codirex.asm.terminal.view;

import org.codirex.asm.terminal.emulator.TerminalEmulator;

/**
 * The screen of one session in the UI process (ADR-0025): an emulator fed from the output stream of
 * the service. It remembers how many bytes of the stream it has seen, so that attaching again
 * continues where it stopped, and it starts from a clear screen when the service no longer has the
 * older output.
 *
 * <p>Output arrives on Binder threads and drawing happens on the UI thread: everything that touches
 * the emulator holds {@link #lock()}.
 */
public final class TerminalScreen {

  /** What happens to the screen. Called with the lock held: do not block. */
  public interface Callbacks {
    /** The screen changed; schedule a redraw. */
    void changed();

    /** The terminal answers the program (cursor position, device attributes). */
    void reply(byte[] data);

    void titleChanged(String title);

    void bell();

    /** The session ended; {@code exitCode} is negative for a signal. */
    void exited(int exitCode);
  }

  private final Object lock = new Object();
  private final TerminalEmulator emulator;
  private final Callbacks callbacks;
  private long seen;
  private boolean exited;
  private int exitCode;

  public TerminalScreen(int rows, int columns, int scrollbackLines, final Callbacks callbacks) {
    this.callbacks = callbacks;
    this.emulator = new TerminalEmulator(rows, columns, scrollbackLines);
    this.emulator.setListener(
        new TerminalEmulator.Listener() {
          @Override
          public void write(byte[] reply) {
            callbacks.reply(reply);
          }

          @Override
          public void titleChanged(String title) {
            callbacks.titleChanged(title);
          }

          @Override
          public void bell() {
            callbacks.bell();
          }
        });
  }

  /** Guards the emulator; hold it while reading {@link #emulator()}. */
  public Object lock() {
    return lock;
  }

  /** Not thread safe: use it only while holding {@link #lock()}. */
  public TerminalEmulator emulator() {
    return emulator;
  }

  /** How many bytes of the session's output this screen has seen. */
  public long seenOffset() {
    synchronized (lock) {
      return seen;
    }
  }

  /** The bytes of the session starting at {@code offset} (see {@code ITerminalListener}). */
  public void onOutput(long offset, byte[] data) {
    synchronized (lock) {
      int skip = 0;
      if (offset > seen) {
        // The service dropped output this screen never saw: start again from a clear screen.
        emulator.reset();
      } else if (offset < seen) {
        skip = (int) Math.min(data.length, seen - offset); // already shown
      }
      if (skip < data.length) {
        emulator.append(data, skip, data.length - skip);
      }
      seen = Math.max(seen, offset + data.length);
      callbacks.changed();
    }
  }

  public void onExit(int code) {
    synchronized (lock) {
      exited = true;
      exitCode = code;
      callbacks.exited(code);
      callbacks.changed();
    }
  }

  public boolean hasExited() {
    synchronized (lock) {
      return exited;
    }
  }

  public int exitCode() {
    synchronized (lock) {
      return exitCode;
    }
  }

  public void resize(int rows, int columns) {
    synchronized (lock) {
      emulator.resize(rows, columns);
      callbacks.changed();
    }
  }
}
