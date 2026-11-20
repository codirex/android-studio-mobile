package org.codirex.asm.domain.lsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public final class LanguageSessionTest {

  /** A server on the other end of two pipes that answers like jdt.ls in the cases tested. */
  private static final class FakeServer {
    final PipedOutputStream toClient = new PipedOutputStream();
    final PipedInputStream fromClientIn;
    final PipedOutputStream fromClientOut = new PipedOutputStream();
    final PipedInputStream clientIn;
    final LinkedBlockingQueue<Map<?, ?>> seen = new LinkedBlockingQueue<>();

    FakeServer() throws Exception {
      clientIn = new PipedInputStream(toClient, 1 << 16);
      fromClientIn = new PipedInputStream(fromClientOut, 1 << 16);
      Thread thread = new Thread(this::serve);
      thread.setDaemon(true);
      thread.start();
    }

    void send(String json) throws Exception {
      byte[] body = json.getBytes("UTF-8");
      toClient.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes("US-ASCII"));
      toClient.write(body);
      toClient.flush();
    }

    private void serve() {
      try {
        String text;
        while ((text = MessageIo.read(fromClientIn)) != null) {
          Map<?, ?> message = (Map<?, ?>) Json.parse(text);
          seen.add(message);
          Object id = message.get("id");
          String method = (String) message.get("method");
          if ("initialize".equals(method)) {
            // The server asks things back before it answers, as jdt.ls does.
            send("{\"id\":900,\"method\":\"workspace/configuration\",\"params\":{\"items\":[{},{}]}}");
            send("{\"id\":" + id + ",\"result\":{\"capabilities\":{}}}");
          } else if ("textDocument/completion".equals(method)) {
            send(
                "{\"id\":"
                    + id
                    + ",\"result\":{\"isIncomplete\":false,\"items\":["
                    + "{\"label\":\"println(String)\",\"kind\":2,\"detail\":\"void\","
                    + "\"textEdit\":{\"newText\":\"println(\",\"range\":{\"start\":{\"line\":3,\"character\":8},\"end\":{\"line\":3,\"character\":10}}}},"
                    + "{\"label\":\"print\",\"insertText\":\"print(\"},"
                    + "{\"label\":\"bare\"},{\"nolabel\":1}]}}");
          } else if ("textDocument/didOpen".equals(method)) {
            send(
                "{\"method\":\"textDocument/publishDiagnostics\",\"params\":{\"uri\":\"file:///A.java\","
                    + "\"diagnostics\":[{\"range\":{\"start\":{\"line\":1,\"character\":2},\"end\":{\"line\":1,\"character\":5}},"
                    + "\"severity\":2,\"message\":\"unused\"},{\"range\":{\"start\":{\"line\":0,\"character\":0},\"end\":{\"line\":0,\"character\":1}},\"message\":\"no severity\"}]}}");
          } else if ("shutdown".equals(method)) {
            send("{\"id\":" + id + ",\"result\":null}");
          }
        }
      } catch (Exception e) {
        // the test ended
      }
    }
  }

  private static final class Collector implements DiagnosticsListener {
    final LinkedBlockingQueue<List<Diagnostic>> lists = new LinkedBlockingQueue<>();

    @Override
    public void onDiagnostics(String uri, List<Diagnostic> diagnostics) {
      assertEquals("file:///A.java", uri);
      lists.add(diagnostics);
    }
  }

  private static LanguageSession session(FakeServer server, Collector collector) {
    LanguageSession session =
        new LanguageSession(server.clientIn, server.fromClientOut, "file:///ws", collector);
    Thread reader = new Thread(session::runReadLoop);
    reader.setDaemon(true);
    reader.start();
    return session;
  }

  @Test
  public void theHandshakeAnswersServerRequestsAndSendsInitialized() throws Exception {
    FakeServer server = new FakeServer();
    LanguageSession session = session(server, new Collector());
    assertFalse(session.isInitialized());
    session.start().get(5, TimeUnit.SECONDS);
    assertTrue(session.isInitialized());

    Map<?, ?> initialize = server.seen.poll(5, TimeUnit.SECONDS);
    assertEquals("initialize", initialize.get("method"));
    assertEquals("file:///ws", ((Map<?, ?>) initialize.get("params")).get("rootUri"));
    // The answer to the server's own request: one null per item.
    Map<?, ?> answer = server.seen.poll(5, TimeUnit.SECONDS);
    assertEquals(900L, answer.get("id"));
    assertEquals(2, ((List<?>) answer.get("result")).size());
    Map<?, ?> initialized = server.seen.poll(5, TimeUnit.SECONDS);
    assertEquals("initialized", initialized.get("method"));
  }

  @Test
  public void completionItemsAreReadWithRangesAndFallbacks() throws Exception {
    FakeServer server = new FakeServer();
    LanguageSession session = session(server, new Collector());
    session.start().get(5, TimeUnit.SECONDS);
    List<CompletionItem> items = session.completion("file:///A.java", 3, 10).get(5, TimeUnit.SECONDS);
    assertEquals(3, items.size());
    CompletionItem first = items.get(0);
    assertEquals("println(String)", first.label());
    assertEquals("void", first.detail());
    assertEquals(2, first.kind());
    assertEquals("println(", first.insertText());
    assertTrue(first.hasRange());
    assertEquals(3, first.startLine());
    assertEquals(8, first.startCharacter());
    assertEquals(10, first.endCharacter());
    assertEquals("print(", items.get(1).insertText());
    assertFalse(items.get(1).hasRange());
    assertEquals("bare", items.get(2).insertText());
  }

  @Test
  public void openingADocumentBringsItsDiagnostics() throws Exception {
    FakeServer server = new FakeServer();
    Collector collector = new Collector();
    LanguageSession session = session(server, collector);
    session.start().get(5, TimeUnit.SECONDS);
    session.open("file:///A.java", "java", 1, "class A {}");
    List<Diagnostic> list = collector.lists.poll(5, TimeUnit.SECONDS);
    assertEquals(2, list.size());
    assertEquals(new Diagnostic(1, 2, 1, 5, Diagnostic.WARNING, "unused"), list.get(0));
    assertEquals(Diagnostic.ERROR, list.get(1).severity());
  }

  @Test
  public void changesAreSentAsTheWholeTextWithTheVersion() throws Exception {
    FakeServer server = new FakeServer();
    LanguageSession session = session(server, new Collector());
    session.start().get(5, TimeUnit.SECONDS);
    session.change("file:///A.java", 7, "new text");
    Map<?, ?> message;
    do {
      message = server.seen.poll(5, TimeUnit.SECONDS);
    } while (!"textDocument/didChange".equals(message.get("method")));
    Map<?, ?> params = (Map<?, ?>) message.get("params");
    assertEquals(7L, ((Map<?, ?>) params.get("textDocument")).get("version"));
    assertEquals("new text", ((Map<?, ?>) ((List<?>) params.get("contentChanges")).get(0)).get("text"));
  }

  @Test
  public void shutdownEndsTheClient() throws Exception {
    FakeServer server = new FakeServer();
    LanguageSession session = session(server, new Collector());
    session.start().get(5, TimeUnit.SECONDS);
    session.shutdown().get(5, TimeUnit.SECONDS);
    assertFalse(session.isInitialized());
  }
}
