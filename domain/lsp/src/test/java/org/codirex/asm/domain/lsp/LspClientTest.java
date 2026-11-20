package org.codirex.asm.domain.lsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public final class LspClientTest {

  private static String frame(String json) {
    return "Content-Length: " + json.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n" + json;
  }

  private static final class Recorder implements LspListener {
    final LinkedBlockingQueue<String> notifications = new LinkedBlockingQueue<>();
    final CountDownLatch closed = new CountDownLatch(1);
    Object requestResult = "ok";

    @Override
    public void onNotification(String method, Object params) {
      notifications.add(method);
    }

    @Override
    public Object onRequest(String method, Object params) throws LspException {
      if (method.equals("unknown")) {
        throw new LspException(LspException.METHOD_NOT_FOUND, "no");
      }
      return requestResult;
    }

    @Override
    public void onClosed(Throwable cause) {
      closed.countDown();
    }
  }

  /** A client wired to pipes, with the "server" ends exposed. */
  private static final class Wire {
    final PipedOutputStream serverToClient = new PipedOutputStream();
    final PipedInputStream clientIn;
    final PipedOutputStream clientOut = new PipedOutputStream();
    final PipedInputStream serverIn;
    final Recorder listener = new Recorder();
    final LspClient client;

    Wire() throws Exception {
      clientIn = new PipedInputStream(serverToClient, 1 << 16);
      serverIn = new PipedInputStream(clientOut, 1 << 16);
      client = new LspClient(clientIn, clientOut, listener);
      Thread reader = new Thread(client::runReadLoop);
      reader.setDaemon(true);
      reader.start();
    }

    void serverSends(String json) throws Exception {
      serverToClient.write(frame(json).getBytes(StandardCharsets.UTF_8));
      serverToClient.flush();
    }

    String serverReads() throws Exception {
      return MessageIo.read(serverIn);
    }
  }

  @Test
  public void aRequestIsAnsweredWithItsResult() throws Exception {
    Wire wire = new Wire();
    CompletableFuture<Object> future = wire.client.request("initialize", null);
    Map<?, ?> sent = (Map<?, ?>) Json.parse(wire.serverReads());
    assertEquals("initialize", sent.get("method"));
    assertEquals("2.0", sent.get("jsonrpc"));
    wire.serverSends("{\"jsonrpc\":\"2.0\",\"id\":" + sent.get("id") + ",\"result\":{\"x\":1}}");
    assertEquals(1L, ((Map<?, ?>) future.get(5, TimeUnit.SECONDS)).get("x"));
  }

  @Test
  public void anErrorAnswerFailsTheFutureWithItsCode() throws Exception {
    Wire wire = new Wire();
    CompletableFuture<Object> future = wire.client.request("m", null);
    Map<?, ?> sent = (Map<?, ?>) Json.parse(wire.serverReads());
    wire.serverSends(
        "{\"id\":" + sent.get("id") + ",\"error\":{\"code\":-32601,\"message\":\"nope\"}}");
    try {
      future.get(5, TimeUnit.SECONDS);
      fail("completed");
    } catch (ExecutionException e) {
      LspException lsp = (LspException) e.getCause();
      assertEquals(-32601, lsp.code());
      assertEquals("nope", lsp.getMessage());
    }
  }

  @Test
  public void answersMayComeOutOfOrder() throws Exception {
    Wire wire = new Wire();
    CompletableFuture<Object> first = wire.client.request("a", null);
    CompletableFuture<Object> second = wire.client.request("b", null);
    Map<?, ?> a = (Map<?, ?>) Json.parse(wire.serverReads());
    Map<?, ?> b = (Map<?, ?>) Json.parse(wire.serverReads());
    wire.serverSends("{\"id\":" + b.get("id") + ",\"result\":\"second\"}");
    wire.serverSends("{\"id\":" + a.get("id") + ",\"result\":\"first\"}");
    assertEquals("first", first.get(5, TimeUnit.SECONDS));
    assertEquals("second", second.get(5, TimeUnit.SECONDS));
  }

  @Test
  public void notificationsReachTheListenerAndOurNotificationsCarryNoId() throws Exception {
    Wire wire = new Wire();
    wire.serverSends("{\"method\":\"textDocument/publishDiagnostics\",\"params\":{}}");
    assertEquals(
        "textDocument/publishDiagnostics", wire.listener.notifications.poll(5, TimeUnit.SECONDS));
    wire.client.notify("initialized", null);
    Map<?, ?> sent = (Map<?, ?>) Json.parse(wire.serverReads());
    assertEquals("initialized", sent.get("method"));
    assertNull(sent.get("id"));
  }

  @Test
  public void aServerRequestIsAnsweredFromTheListener() throws Exception {
    Wire wire = new Wire();
    wire.serverSends("{\"id\":7,\"method\":\"window/workDoneProgress/create\",\"params\":{}}");
    Map<?, ?> reply = (Map<?, ?>) Json.parse(wire.serverReads());
    assertEquals(7L, reply.get("id"));
    assertEquals("ok", reply.get("result"));
    wire.serverSends("{\"id\":8,\"method\":\"unknown\"}");
    Map<?, ?> error = (Map<?, ?>) Json.parse(wire.serverReads());
    assertEquals(8L, error.get("id"));
    assertEquals(-32601L, ((Map<?, ?>) error.get("error")).get("code"));
  }

  @Test
  public void whenTheStreamEndsWaitingRequestsFailAndTheListenerIsTold() throws Exception {
    Wire wire = new Wire();
    CompletableFuture<Object> future = wire.client.request("slow", null);
    wire.serverReads();
    wire.serverToClient.close();
    try {
      future.get(5, TimeUnit.SECONDS);
      fail("completed");
    } catch (ExecutionException e) {
      assertEquals(LspException.CONNECTION, ((LspException) e.getCause()).code());
    }
    assertTrue(wire.listener.closed.await(5, TimeUnit.SECONDS));
    try {
      wire.client.request("late", null).get(5, TimeUnit.SECONDS);
      fail("completed");
    } catch (ExecutionException e) {
      assertEquals(LspException.CONNECTION, ((LspException) e.getCause()).code());
    }
  }

  @Test
  public void framingHandlesNonAsciiAndRefusesBadHeaders() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    MessageIo.write(out, "{\"t\":\"é中\"}");
    assertEquals(
        "{\"t\":\"é中\"}", MessageIo.read(new ByteArrayInputStream(out.toByteArray())));
    assertNull(MessageIo.read(new ByteArrayInputStream(new byte[0])));
    String[] bad = {"Content-Length: x\r\n\r\n", "Foo: 1\r\n\r\n", "Content-Length: 5\r\n\r\nab"};
    for (String text : bad) {
      try {
        MessageIo.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII)));
        fail("read: " + text);
      } catch (java.io.IOException expected) {
        // expected
      }
    }
  }
}
