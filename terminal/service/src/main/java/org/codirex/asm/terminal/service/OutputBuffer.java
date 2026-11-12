package org.codirex.asm.terminal.service;

/**
 * The most recent output of a session, kept so that a UI that attaches later can rebuild its screen
 * (ADR-0024). Offsets count every byte ever appended, so a reader can say where it stopped. Not
 * thread safe: the session guards it.
 */
final class OutputBuffer {

  private final byte[] data;
  private long end;

  OutputBuffer(int capacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity " + capacity);
    }
    data = new byte[capacity];
  }

  /** The offset after the last byte. */
  long end() {
    return end;
  }

  /** The offset of the oldest byte that is still kept. */
  long start() {
    return Math.max(0, end - data.length);
  }

  void append(byte[] bytes, int offset, int length) {
    if (length > data.length) {
      // Only the last `capacity` bytes can stay; the rest counts as written and dropped.
      int dropped = length - data.length;
      end += dropped;
      offset += dropped;
      length = data.length;
    }
    int at = (int) (end % data.length);
    int first = Math.min(length, data.length - at);
    System.arraycopy(bytes, offset, data, at, first);
    System.arraycopy(bytes, offset + first, data, 0, length - first);
    end += length;
  }

  /**
   * The bytes from {@code offset} to the end. If {@code offset} is older than what is kept, the
   * result starts at {@link #start()}; if it is past the end, the result is empty.
   */
  Chunk readFrom(long offset) {
    long from = Math.max(offset, start());
    if (from >= end) {
      return new Chunk(end, new byte[0]);
    }
    int length = (int) (end - from);
    byte[] out = new byte[length];
    int at = (int) (from % data.length);
    int first = Math.min(length, data.length - at);
    System.arraycopy(data, at, out, 0, first);
    System.arraycopy(data, 0, out, first, length - first);
    return new Chunk(from, out);
  }

  /** Bytes and the offset of the first one. */
  static final class Chunk {
    final long offset;
    final byte[] bytes;

    Chunk(long offset, byte[] bytes) {
      this.offset = offset;
      this.bytes = bytes;
    }
  }
}
