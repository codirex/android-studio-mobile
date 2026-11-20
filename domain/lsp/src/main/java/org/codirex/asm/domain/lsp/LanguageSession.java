package org.codirex.asm.domain.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * One conversation with a language server about one workspace: the handshake, the documents the
 * person has open, completion and diagnostics (ADR-0031). It sits on {@link LspClient} and knows no
 * server in particular.
 *
 * <p>{@link #runReadLoop()} blocks: call it from a background job. The other methods may be called
 * from any thread. Documents are sent whole on every change (full synchronization), which every
 * server accepts.
 */
public final class LanguageSession {

  private final LspClient client;
  private final String rootUri;
  private final DiagnosticsListener diagnostics;
  private volatile boolean initialized;

  public LanguageSession(
      InputStream in, OutputStream out, String rootUri, DiagnosticsListener diagnostics) {
    this.rootUri = rootUri;
    this.diagnostics = diagnostics;
    this.client = new LspClient(in, out, new Handler());
  }

  /** Reads from the server until the connection ends. */
  public void runReadLoop() {
    client.runReadLoop();
  }

  /** Whether the handshake is done: documents can be opened and completion asked. */
  public boolean isInitialized() {
    return initialized;
  }

  /** Sends {@code initialize}; the future completes when the server has answered and is told. */
  public CompletableFuture<Void> start() {
    Map<String, Object> synchronization = new LinkedHashMap<>();
    synchronization.put("dynamicRegistration", false);
    synchronization.put("didSave", false);
    Map<String, Object> completionItem = new LinkedHashMap<>();
    completionItem.put("snippetSupport", false);
    Map<String, Object> completion = new LinkedHashMap<>();
    completion.put("dynamicRegistration", false);
    completion.put("completionItem", completionItem);
    Map<String, Object> publish = new LinkedHashMap<>();
    publish.put("relatedInformation", false);
    Map<String, Object> textDocument = new LinkedHashMap<>();
    textDocument.put("synchronization", synchronization);
    textDocument.put("completion", completion);
    textDocument.put("publishDiagnostics", publish);
    Map<String, Object> workspace = new LinkedHashMap<>();
    workspace.put("configuration", true);
    workspace.put("workspaceFolders", true);
    Map<String, Object> capabilities = new LinkedHashMap<>();
    capabilities.put("textDocument", textDocument);
    capabilities.put("workspace", workspace);
    Map<String, Object> folder = new LinkedHashMap<>();
    folder.put("uri", rootUri);
    folder.put("name", "workspace");
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("processId", null);
    params.put("rootUri", rootUri);
    params.put("capabilities", capabilities);
    params.put("workspaceFolders", Collections.singletonList(folder));
    return client
        .request("initialize", params)
        .thenAccept(
            result -> {
              try {
                client.notify("initialized", new LinkedHashMap<String, Object>());
              } catch (IOException e) {
                throw new java.util.concurrent.CompletionException(
                    new LspException(LspException.CONNECTION, "could not send initialized", e));
              }
              initialized = true;
            });
  }

  /** Tells the server a document is open with this text. */
  public void open(String uri, String languageId, int version, String text) throws IOException {
    Map<String, Object> document = new LinkedHashMap<>();
    document.put("uri", uri);
    document.put("languageId", languageId);
    document.put("version", (long) version);
    document.put("text", text);
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("textDocument", document);
    client.notify("textDocument/didOpen", params);
  }

  /** Tells the server the whole new text of an open document. */
  public void change(String uri, int version, String text) throws IOException {
    Map<String, Object> document = new LinkedHashMap<>();
    document.put("uri", uri);
    document.put("version", (long) version);
    Map<String, Object> change = new LinkedHashMap<>();
    change.put("text", text);
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("textDocument", document);
    params.put("contentChanges", Collections.singletonList(change));
    client.notify("textDocument/didChange", params);
  }

  /** Tells the server a document is closed. */
  public void close(String uri) throws IOException {
    Map<String, Object> document = new LinkedHashMap<>();
    document.put("uri", uri);
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("textDocument", document);
    client.notify("textDocument/didClose", params);
  }

  /** Asks for the proposals at a position (zero based). */
  public CompletableFuture<List<CompletionItem>> completion(String uri, int line, int character) {
    Map<String, Object> document = new LinkedHashMap<>();
    document.put("uri", uri);
    Map<String, Object> position = new LinkedHashMap<>();
    position.put("line", (long) line);
    position.put("character", (long) character);
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("textDocument", document);
    params.put("position", position);
    return client.request("textDocument/completion", params).thenApply(LanguageSession::items);
  }

  /** Ends the conversation politely: {@code shutdown}, then {@code exit}. */
  public CompletableFuture<Void> shutdown() {
    return client
        .request("shutdown", null)
        .handle(
            (result, failure) -> {
              try {
                client.notify("exit", null);
              } catch (IOException ignored) {
                // The server is gone already.
              }
              client.close();
              return null;
            });
  }

  /** Ends the conversation without waiting for the server. */
  public void abort() {
    client.close();
  }

  // ---- reading what the server says ----

  static List<CompletionItem> items(Object result) {
    Object list = result;
    if (result instanceof Map) {
      list = ((Map<?, ?>) result).get("items");
    }
    List<CompletionItem> items = new ArrayList<>();
    if (!(list instanceof List)) {
      return items;
    }
    for (Object entry : (List<?>) list) {
      if (entry instanceof Map) {
        CompletionItem item = item((Map<?, ?>) entry);
        if (item != null) {
          items.add(item);
        }
      }
    }
    return items;
  }

  private static CompletionItem item(Map<?, ?> map) {
    Object label = map.get("label");
    if (!(label instanceof String)) {
      return null;
    }
    String insert = text(map.get("insertText"), (String) label);
    int startLine = -1;
    int startCharacter = -1;
    int endLine = -1;
    int endCharacter = -1;
    Object edit = map.get("textEdit");
    if (edit instanceof Map) {
      Map<?, ?> textEdit = (Map<?, ?>) edit;
      insert = text(textEdit.get("newText"), insert);
      Object range = textEdit.get("range");
      if (range == null) {
        range = textEdit.get("replace"); // InsertReplaceEdit
      }
      if (range instanceof Map) {
        Map<?, ?> r = (Map<?, ?>) range;
        startLine = number(position(r.get("start"), "line"), -1);
        startCharacter = number(position(r.get("start"), "character"), -1);
        endLine = number(position(r.get("end"), "line"), -1);
        endCharacter = number(position(r.get("end"), "character"), -1);
        if (startLine < 0 || startCharacter < 0 || endLine < 0 || endCharacter < 0) {
          startLine = -1;
        }
      }
    }
    return new CompletionItem(
        (String) label,
        text(map.get("detail"), ""),
        number(map.get("kind"), 0),
        insert,
        startLine,
        startCharacter,
        endLine,
        endCharacter);
  }

  static List<Diagnostic> diagnosticsOf(Object list) {
    List<Diagnostic> result = new ArrayList<>();
    if (!(list instanceof List)) {
      return result;
    }
    for (Object entry : (List<?>) list) {
      if (!(entry instanceof Map)) {
        continue;
      }
      Map<?, ?> map = (Map<?, ?>) entry;
      Object range = map.get("range");
      if (!(range instanceof Map)) {
        continue;
      }
      Map<?, ?> r = (Map<?, ?>) range;
      result.add(
          new Diagnostic(
              number(position(r.get("start"), "line"), 0),
              number(position(r.get("start"), "character"), 0),
              number(position(r.get("end"), "line"), 0),
              number(position(r.get("end"), "character"), 0),
              number(map.get("severity"), Diagnostic.ERROR),
              text(map.get("message"), "")));
    }
    return result;
  }

  private static Object position(Object position, String field) {
    return position instanceof Map ? ((Map<?, ?>) position).get(field) : null;
  }

  private static String text(Object value, String fallback) {
    return value instanceof String ? (String) value : fallback;
  }

  private static int number(Object value, int fallback) {
    return value instanceof Number ? ((Number) value).intValue() : fallback;
  }

  /** What the server starts: diagnostics, and the requests every server sends at the start. */
  private final class Handler implements LspListener {

    @Override
    public void onNotification(String method, Object params) {
      if (!"textDocument/publishDiagnostics".equals(method) || !(params instanceof Map)) {
        return;
      }
      Map<?, ?> map = (Map<?, ?>) params;
      Object uri = map.get("uri");
      if (uri instanceof String) {
        diagnostics.onDiagnostics((String) uri, diagnosticsOf(map.get("diagnostics")));
      }
    }

    @Override
    public Object onRequest(String method, Object params) throws LspException {
      switch (method) {
        case "client/registerCapability":
        case "client/unregisterCapability":
        case "window/workDoneProgress/create":
        case "window/showMessageRequest":
          return null;
        case "workspace/configuration":
          // One value per item asked for; null means "no setting, use your default".
          List<Object> answers = new ArrayList<>();
          if (params instanceof Map && ((Map<?, ?>) params).get("items") instanceof List) {
            for (Object ignored : (List<?>) ((Map<?, ?>) params).get("items")) {
              answers.add(null);
            }
          }
          return answers;
        case "workspace/workspaceFolders":
          Map<String, Object> folder = new LinkedHashMap<>();
          folder.put("uri", rootUri);
          folder.put("name", "workspace");
          return Collections.singletonList(folder);
        default:
          throw new LspException(LspException.METHOD_NOT_FOUND, "not supported: " + method);
      }
    }

    @Override
    public void onClosed(Throwable cause) {
      initialized = false;
    }
  }
}
