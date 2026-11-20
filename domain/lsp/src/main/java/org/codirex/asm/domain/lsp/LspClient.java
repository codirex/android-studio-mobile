package org.codirex.asm.domain.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The client side of one language server connection (JSON-RPC 2.0 over the LSP framing), on two
 * streams. It knows nothing about a particular server or where the streams come from.
 *
 * <p>{@link #runReadLoop()} blocks and reads until the stream ends: call it from a background job.
 * {@link #request} and {@link #notify} may be called from any thread. Requests give a future that
 * is completed by the read loop; when the connection ends, every request still waiting fails with
 * an {@link LspException}.
 */
public final class LspClient {

  private final InputStream in;
  private final OutputStream out;
  private final LspListener listener;
  private final Object writeLock = new Object();
  private final AtomicLong nextId = new AtomicLong(1);
  private final Map<Long, CompletableFuture<Object>> pending = new ConcurrentHashMap<>();
  private final AtomicBoolean closed = new AtomicBoolean();

  public LspClient(InputStream in, OutputStream out, LspListener listener) {
    this.in = Objects.requireNonNull(in, "in");
    this.out = Objects.requireNonNull(out, "out");
    this.listener = Objects.requireNonNull(listener, "listener");
  }

  /** Sends a request. The future yields the {@code result} of the answer (may be null). */
  public CompletableFuture<Object> request(String method, Object params) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    if (closed.get()) {
      future.completeExceptionally(new LspException(LspException.CONNECTION, "connection closed"));
      return future;
    }
    long id = nextId.getAndIncrement();
    pending.put(id, future);
    Map<String, Object> message = envelope();
    message.put("id", id);
    message.put("method", method);
    if (params != null) {
      message.put("params", params);
    }
    try {
      send(message);
    } catch (IOException | JsonException e) {
      pending.remove(id);
      future.completeExceptionally(
          new LspException(LspException.CONNECTION, "could not send " + method, e));
    }
    // The connection may have ended between the check above and the registration.
    if (closed.get() && pending.remove(id) != null) {
      future.completeExceptionally(new LspException(LspException.CONNECTION, "connection closed"));
    }
    return future;
  }

  /** Sends a notification. Nothing comes back. */
  public void notify(String method, Object params) throws IOException {
    Map<String, Object> message = envelope();
    message.put("method", method);
    if (params != null) {
      message.put("params", params);
    }
    try {
      send(message);
    } catch (JsonException e) {
      throw new IOException("could not write " + method, e);
    }
  }

  /** Reads and handles messages until the stream ends or fails; then closes the client. */
  public void runReadLoop() {
    Throwable cause = null;
    try {
      String text;
      while ((text = MessageIo.read(in)) != null) {
        handle(text);
      }
    } catch (IOException e) {
      cause = e;
    } catch (RuntimeException e) {
      cause = e;
    } finally {
      shutdown(cause);
    }
  }

  /** Ends the client: waiting requests fail, the listener is told. The streams are not closed. */
  public void close() {
    shutdown(null);
  }

  private void shutdown(Throwable cause) {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    LspException failure = new LspException(LspException.CONNECTION, "connection closed", cause);
    for (Long id : pending.keySet()) {
      CompletableFuture<Object> future = pending.remove(id);
      if (future != null) {
        future.completeExceptionally(failure);
      }
    }
    listener.onClosed(cause);
  }

  private void handle(String text) {
    Object parsed;
    try {
      parsed = Json.parse(text);
    } catch (JsonException e) {
      return; // A message that is not JSON cannot be answered; skip it.
    }
    if (!(parsed instanceof Map)) {
      return;
    }
    Map<?, ?> message = (Map<?, ?>) parsed;
    Object method = message.get("method");
    Object id = message.get("id");
    if (method instanceof String) {
      if (id == null) {
        listener.onNotification((String) method, message.get("params"));
      } else {
        answerServerRequest(id, (String) method, message.get("params"));
      }
    } else if (id instanceof Number) {
      complete(((Number) id).longValue(), message);
    }
  }

  private void complete(long id, Map<?, ?> message) {
    CompletableFuture<Object> future = pending.remove(id);
    if (future == null) {
      return;
    }
    Object error = message.get("error");
    if (error instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) error;
      Object code = map.get("code");
      Object text = map.get("message");
      future.completeExceptionally(
          new LspException(
              code instanceof Number ? ((Number) code).intValue() : LspException.INTERNAL_ERROR,
              text instanceof String ? (String) text : "error"));
    } else {
      future.complete(message.get("result"));
    }
  }

  private void answerServerRequest(Object id, String method, Object params) {
    Map<String, Object> reply = envelope();
    reply.put("id", id);
    try {
      reply.put("result", listener.onRequest(method, params));
    } catch (LspException e) {
      reply.put("error", error(e.code(), e.getMessage()));
    } catch (RuntimeException e) {
      reply.put("error", error(LspException.INTERNAL_ERROR, String.valueOf(e.getMessage())));
    }
    try {
      send(reply);
    } catch (IOException | JsonException e) {
      // The read loop notices a broken connection on its own.
    }
  }

  private static Map<String, Object> error(int code, String message) {
    Map<String, Object> error = new LinkedHashMap<>();
    error.put("code", (long) code);
    error.put("message", message == null ? "" : message);
    return error;
  }

  private static Map<String, Object> envelope() {
    Map<String, Object> message = new LinkedHashMap<>();
    message.put("jsonrpc", "2.0");
    return message;
  }

  private void send(Map<String, Object> message) throws IOException, JsonException {
    String text = Json.write(message);
    synchronized (writeLock) {
      MessageIo.write(out, text);
    }
  }
}
