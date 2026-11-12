package org.codirex.asm.terminal.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/** A program for tests: output is what the test feeds, input is recorded. */
final class FakeProcess implements TerminalProcess {

  private static final byte[] END = new byte[0];

  private final BlockingQueue<byte[]> output = new LinkedBlockingQueue<>();
  private final CountDownLatch ended = new CountDownLatch(1);
  private final ByteArrayOutputStream input = new ByteArrayOutputStream();
  private volatile int exitCode;
  volatile int rows;
  volatile int columns;
  volatile int lastSignal;
  volatile boolean closed;
  /** When true, the output only ends when the process is closed (a background job holds the tty). */
  volatile boolean holdOutputOpen;

  void print(String text) {
    output.add(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /** The program ends with {@code code}; its output ends too unless {@link #holdOutputOpen}. */
  void exit(int code) {
    exitCode = code;
    ended.countDown();
    if (!holdOutputOpen) {
      output.add(END);
    }
  }

  synchronized String typed() {
    return new String(input.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    try {
      byte[] next = output.take();
      if (next == END) {
        output.add(END);
        return -1;
      }
      System.arraycopy(next, 0, buffer, offset, next.length);
      return next.length;
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  @Override
  public synchronized void write(byte[] buffer, int offset, int length) {
    input.write(buffer, offset, length);
  }

  @Override
  public void resize(int rows, int columns) {
    this.rows = rows;
    this.columns = columns;
  }

  @Override
  public void signal(int signal) {
    lastSignal = signal;
  }

  @Override
  public int waitFor() {
    try {
      ended.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return exitCode;
  }

  @Override
  public void close() {
    closed = true;
    exitCode = exitCode == 0 && ended.getCount() > 0 ? -9 : exitCode;
    ended.countDown();
    output.add(END);
  }
}
