package org.codirex.asm.domain.lsp;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * The framing of the language server protocol: a header {@code Content-Length: N}, an empty line,
 * then N bytes of UTF-8 JSON.
 */
final class MessageIo {

  /** A message larger than this is refused (the biggest answers are completion lists). */
  static final int MAX_MESSAGE_BYTES = 64 * 1024 * 1024;

  private static final int MAX_HEADER_LINE = 8 * 1024;

  private MessageIo() {}

  /**
   * Reads the next message. Returns {@code null} at the end of the stream between two messages; an
   * end inside a message is an {@link EOFException}.
   */
  static String read(InputStream in) throws IOException {
    int length = -1;
    boolean any = false;
    while (true) {
      String line = readLine(in);
      if (line == null) {
        if (any) {
          throw new EOFException("stream ended inside a header");
        }
        return null;
      }
      any = true;
      if (line.isEmpty()) {
        break;
      }
      int colon = line.indexOf(':');
      if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Content-Length")) {
        try {
          length = Integer.parseInt(line.substring(colon + 1).trim());
        } catch (NumberFormatException e) {
          throw new IOException("bad Content-Length: " + line);
        }
      }
    }
    if (length < 0) {
      throw new IOException("a message without Content-Length");
    }
    if (length > MAX_MESSAGE_BYTES) {
      throw new IOException("message too large: " + length);
    }
    byte[] body = new byte[length];
    int done = 0;
    while (done < length) {
      int read = in.read(body, done, length - done);
      if (read < 0) {
        throw new EOFException("stream ended inside a message");
      }
      done += read;
    }
    return new String(body, StandardCharsets.UTF_8);
  }

  /** Writes one message and flushes. The caller serializes writers. */
  static void write(OutputStream out, String json) throws IOException {
    byte[] body = json.getBytes(StandardCharsets.UTF_8);
    out.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
    out.write(body);
    out.flush();
  }

  /** A header line without its line end, or {@code null} at the end of the stream. */
  private static String readLine(InputStream in) throws IOException {
    ByteArrayOutputStream line = new ByteArrayOutputStream();
    int b = in.read();
    if (b < 0) {
      return null;
    }
    while (b >= 0 && b != '\n') {
      if (b != '\r') {
        line.write(b);
      }
      if (line.size() > MAX_HEADER_LINE) {
        throw new IOException("header line too long");
      }
      b = in.read();
    }
    return new String(line.toByteArray(), StandardCharsets.US_ASCII);
  }
}
