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
  private final Map<String, Object> initializationOptions;
  private final DiagnosticsListener diagnostics;
  private volatile boolean initialized;

  public LanguageSession(
      InputStream in, OutputStream out, String rootUri, DiagnosticsListener diagnostics) {
    this(in, out, rootUri, null, diagnostics);
  }

  /**
   * @param initializationOptions server specific settings sent with {@code initialize}, or null
   */
  public LanguageSession(
      InputStream in,
      OutputStream out,
      String rootUri,
      Map<String, Object> initializationOptions,
      DiagnosticsListener diagnostics) {
    this.rootUri = rootUri;
    this.initializationOptions = initializationOptions;
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
    Map<String, Object> hover = new LinkedHashMap<>();
    hover.put("dynamicRegistration", false);
    hover.put("contentFormat", java.util.Arrays.asList("plaintext", "markdown"));
    textDocument.put("hover", hover);
    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put("dynamicRegistration", false);
    definition.put("linkSupport", true);
    textDocument.put("definition", definition);
    Map<String, Object> formatting = new LinkedHashMap<>();
    formatting.put("dynamicRegistration", false);
    textDocument.put("formatting", formatting);
    Map<String, Object> documentSymbol = new LinkedHashMap<>();
    documentSymbol.put("dynamicRegistration", false);
    documentSymbol.put("hierarchicalDocumentSymbolSupport", true);
    textDocument.put("documentSymbol", documentSymbol);
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
    if (initializationOptions != null) {
      params.put("initializationOptions", initializationOptions);
    }
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

  /** Asks what the server knows about the symbol at a position (zero based): plain text, or empty. */
  public CompletableFuture<String> hover(String uri, int line, int character) {
    return client
        .request("textDocument/hover", positionParams(uri, line, character))
        .thenApply(LanguageSession::hoverOf);
  }

  /** Asks where the symbol at a position (zero based) is defined. */
  public CompletableFuture<List<Location>> definition(String uri, int line, int character) {
    return client
        .request("textDocument/definition", positionParams(uri, line, character))
        .thenApply(LanguageSession::locationsOf);
  }

  /** Asks how the server would format the whole document: the edits to apply, last to first. */
  public CompletableFuture<List<TextEdit>> format(String uri, int tabSize) {
    Map<String, Object> document = new LinkedHashMap<>();
    document.put("uri", uri);
    Map<String, Object> options = new LinkedHashMap<>();
    options.put("tabSize", (long) tabSize);
    options.put("insertSpaces", true);
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("textDocument", document);
    params.put("options", options);
    return client.request("textDocument/formatting", params).thenApply(LanguageSession::editsOf);
  }

  /** Asks for the symbols of a document, in the order they are declared. */
  public CompletableFuture<List<DocumentSymbol>> documentSymbols(String uri) {
    Map<String, Object> document = new LinkedHashMap<>();
    document.put("uri", uri);
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("textDocument", document);
    return client
        .request("textDocument/documentSymbol", params)
        .thenApply(LanguageSession::symbolsOf);
  }

  private static Map<String, Object> positionParams(String uri, int line, int character) {
    Map<String, Object> document = new LinkedHashMap<>();
    document.put("uri", uri);
    Map<String, Object> position = new LinkedHashMap<>();
    position.put("line", (long) line);
    position.put("character", (long) character);
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("textDocument", document);
    params.put("position", position);
    return params;
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

  /** The text of a hover answer: a string, MarkupContent, MarkedString or a list of those. */
  static String hoverOf(Object result) {
    if (!(result instanceof Map)) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    addContents(((Map<?, ?>) result).get("contents"), out);
    return out.toString().trim();
  }

  private static void addContents(Object contents, StringBuilder out) {
    String piece = null;
    if (contents instanceof String) {
      piece = (String) contents;
    } else if (contents instanceof Map) {
      Object value = ((Map<?, ?>) contents).get("value");
      piece = value instanceof String ? (String) value : null;
    } else if (contents instanceof List) {
      for (Object entry : (List<?>) contents) {
        addContents(entry, out);
      }
      return;
    }
    if (piece == null) {
      return;
    }
    StringBuilder clean = new StringBuilder();
    for (String line : piece.split("\\r?\\n", -1)) {
      // Code fences are Markdown decoration; the screen shows plain text.
      if (!line.trim().startsWith("```")) {
        clean.append(line).append('\n');
      }
    }
    String text = clean.toString().trim();
    if (!text.isEmpty()) {
      if (out.length() > 0) {
        out.append("\n\n");
      }
      out.append(text);
    }
  }

  /** Location, Location[] or LocationLink[]; anything else gives an empty list. */
  static List<Location> locationsOf(Object result) {
    List<Location> found = new ArrayList<>();
    if (result instanceof Map) {
      addLocation((Map<?, ?>) result, found);
    } else if (result instanceof List) {
      for (Object entry : (List<?>) result) {
        if (entry instanceof Map) {
          addLocation((Map<?, ?>) entry, found);
        }
      }
    }
    return found;
  }

  private static void addLocation(Map<?, ?> map, List<Location> found) {
    Object uri = map.get("uri");
    Object range = map.get("range");
    if (!(uri instanceof String)) {
      // A LocationLink names its target instead.
      uri = map.get("targetUri");
      range = map.get("targetSelectionRange");
      if (range == null) {
        range = map.get("targetRange");
      }
    }
    if (uri instanceof String && range instanceof Map) {
      Object start = ((Map<?, ?>) range).get("start");
      found.add(
          new Location(
              (String) uri, number(position(start, "line"), 0), number(position(start, "character"), 0)));
    }
  }

  /** The edits of a formatting answer, sorted so that applying them one after the other is safe. */
  static List<TextEdit> editsOf(Object result) {
    List<TextEdit> edits = new ArrayList<>();
    if (!(result instanceof List)) {
      return edits;
    }
    for (Object entry : (List<?>) result) {
      if (!(entry instanceof Map)) {
        continue;
      }
      Map<?, ?> map = (Map<?, ?>) entry;
      Object range = map.get("range");
      if (!(range instanceof Map) || !(map.get("newText") instanceof String)) {
        continue;
      }
      Map<?, ?> r = (Map<?, ?>) range;
      edits.add(
          new TextEdit(
              number(position(r.get("start"), "line"), 0),
              number(position(r.get("start"), "character"), 0),
              number(position(r.get("end"), "line"), 0),
              number(position(r.get("end"), "character"), 0),
              (String) map.get("newText")));
    }
    // Last position first: an edit then never moves the places of those still to apply.
    Collections.sort(
        edits,
        (a, b) ->
            a.startLine() != b.startLine()
                ? Integer.compare(b.startLine(), a.startLine())
                : Integer.compare(b.startCharacter(), a.startCharacter()));
    return edits;
  }

  /**
   * The symbols of a documentSymbol answer, flattened in declaration order: DocumentSymbol[] (with
   * children) or SymbolInformation[]. A symbol inside another names it as its container.
   */
  static List<DocumentSymbol> symbolsOf(Object result) {
    List<DocumentSymbol> symbols = new ArrayList<>();
    if (result instanceof List) {
      addSymbols((List<?>) result, "", symbols);
    }
    return symbols;
  }

  private static void addSymbols(List<?> list, String container, List<DocumentSymbol> out) {
    for (Object entry : list) {
      if (!(entry instanceof Map)) {
        continue;
      }
      Map<?, ?> map = (Map<?, ?>) entry;
      if (!(map.get("name") instanceof String)) {
        continue;
      }
      Object range = map.get("selectionRange");
      if (range == null) {
        range = map.get("range");
      }
      if (range == null && map.get("location") instanceof Map) {
        range = ((Map<?, ?>) map.get("location")).get("range");
      }
      if (!(range instanceof Map)) {
        continue;
      }
      Object start = ((Map<?, ?>) range).get("start");
      String parent = container;
      if (parent.isEmpty() && map.get("containerName") instanceof String) {
        parent = (String) map.get("containerName");
      }
      String name = (String) map.get("name");
      out.add(
          new DocumentSymbol(
              name,
              text(map.get("detail"), ""),
              number(map.get("kind"), 0),
              number(position(start, "line"), 0),
              number(position(start, "character"), 0),
              parent));
      if (map.get("children") instanceof List) {
        addSymbols((List<?>) map.get("children"), name, out);
      }
    }
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
