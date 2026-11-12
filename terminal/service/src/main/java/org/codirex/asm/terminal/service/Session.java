package org.codirex.asm.terminal.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * One shell: it reads the program's output on its own thread, keeps the recent part for UIs that
 * attach later and tells the attached listeners (ADR-0024).
 *
 * <p>Output and attaching are serialised by one lock, so a listener sees the replay and the live
 * output in order, with no gap and no repeat. Listeners must not block: the Binder listeners are
 * one way.
 */
final class Session {

  /** Where a session reports to; a failure of a listener (a dead UI process) removes it. */
  interface Listener {
    void onOutput(int session, long offset, byte[] data);

    void onExit(int session, int exitCode);
  }

  private static final int READ_SIZE = 16 * 1024;
  /** How long output that is still on its way is awaited after the program has ended. */
  private static final long DRAIN_MILLIS = 300;

  private final int id;
  private final TerminalProcess process;
  private final Object lock = new Object();
  private final OutputBuffer buffer;
  private final List<Listener> listeners = new ArrayList<>();
  private boolean exited;
  private int exitCode;
  private Thread reader;

  Session(int id, TerminalProcess process, int bufferCapacity) {
    this.id = id;
    this.process = process;
    this.buffer = new OutputBuffer(bufferCapacity);
  }

  int id() {
    return id;
  }

  /** Starts the threads that read the output and wait for the end. Call once. */
  void start() {
    reader = new Thread(this::pump, "terminal-reader-" + id);
    reader.setDaemon(true);
    Thread waiter = new Thread(this::awaitExit, "terminal-waiter-" + id);
    waiter.setDaemon(true);
    reader.start();
    waiter.start();
  }

  private void pump() {
    byte[] bytes = new byte[READ_SIZE];
    try {
      int count;
      while ((count = process.read(bytes, 0, bytes.length)) >= 0) {
        if (count == 0) {
          continue;
        }
        byte[] copy = new byte[count];
        System.arraycopy(bytes, 0, copy, 0, count);
        synchronized (lock) {
          long offset = buffer.end();
          buffer.append(copy, 0, count);
          deliver(offset, copy);
        }
      }
    } catch (IOException e) {
      // The terminal is gone; the waiter reports the end.
    }
  }

  private void awaitExit() {
    int code = process.waitFor();
    try {
      reader.join(DRAIN_MILLIS);
      if (reader.isAlive()) {
        // Something that the shell started still holds the terminal: end it, as closing a window does.
        process.close();
        reader.join(1_000);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    synchronized (lock) {
      exited = true;
      exitCode = code;
      for (Listener listener : new ArrayList<>(listeners)) {
        try {
          listener.onExit(id, code);
        } catch (RuntimeException e) {
          listeners.remove(listener);
        }
      }
    }
  }

  /** Must hold the lock. */
  private void deliver(long offset, byte[] data) {
    for (Listener listener : new ArrayList<>(listeners)) {
      try {
        listener.onOutput(id, offset, data);
      } catch (RuntimeException e) {
        listeners.remove(listener);
      }
    }
  }

  /**
   * Sends the output from {@code fromOffset} on (what is still kept) to the listener, then
   * everything new, and the exit if the session has ended.
   */
  void attach(Listener listener, long fromOffset) {
    synchronized (lock) {
      OutputBuffer.Chunk replay = buffer.readFrom(fromOffset);
      try {
        if (replay.bytes.length > 0) {
          listener.onOutput(id, replay.offset, replay.bytes);
        }
        if (exited) {
          listener.onExit(id, exitCode);
          return;
        }
      } catch (RuntimeException e) {
        return;
      }
      if (!listeners.contains(listener)) {
        listeners.add(listener);
      }
    }
  }

  void detach(Listener listener) {
    synchronized (lock) {
      listeners.remove(listener);
    }
  }

  void write(byte[] data) throws IOException {
    process.write(data, 0, data.length);
  }

  void resize(int rows, int columns) throws IOException {
    process.resize(rows, columns);
  }

  void signal(int signal) throws IOException {
    process.signal(signal);
  }

  boolean hasExited() {
    synchronized (lock) {
      return exited;
    }
  }

  /** Ends the program; the waiter then reports the exit. */
  void close() {
    process.close();
  }
}
