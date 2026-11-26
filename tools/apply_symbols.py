"""One-off: connects symbol search (@) of the command palette to the language server client, the editor, the workspace and the activity."""
import pathlib


def before(path, anchor, block):
    p = pathlib.Path(path)
    s = p.read_text(encoding="utf-8")
    assert s.count(anchor) == 1, (path, anchor[:80], s.count(anchor))
    p.write_text(s.replace(anchor, block + anchor), encoding="utf-8")


def replace(path, old, new):
    p = pathlib.Path(path)
    s = p.read_text(encoding="utf-8")
    assert s.count(old) == 1, (path, old[:80], s.count(old))
    p.write_text(s.replace(old, new), encoding="utf-8")


APP = "app/src/main/java/org/codirex/asm/"
LSP = "domain/lsp/src/main/java/org/codirex/asm/domain/lsp/LanguageSession.java"
SERVER = "feature/editor/src/main/java/org/codirex/asm/feature/editor/JavaLanguageServer.java"
EDITOR = "feature/editor/src/main/java/org/codirex/asm/feature/editor/EditorFragment.java"

# ---- app depends on domain:lsp (the palette lists DocumentSymbol) ----
replace("app/build.gradle.kts",
        '    implementation(project(":domain:filetree"))',
        '    implementation(project(":domain:filetree"))\n    implementation(project(":domain:lsp"))')

# ---- domain:lsp ----
before(LSP, '    textDocument.put("publishDiagnostics", publish);', r'''    Map<String, Object> documentSymbol = new LinkedHashMap<>();
    documentSymbol.put("dynamicRegistration", false);
    documentSymbol.put("hierarchicalDocumentSymbolSupport", true);
    textDocument.put("documentSymbol", documentSymbol);
''')

before(LSP, '  private static Map<String, Object> positionParams(', r'''  /** Asks for the symbols of a document, in the order they are declared. */
  public CompletableFuture<List<DocumentSymbol>> documentSymbols(String uri) {
    Map<String, Object> document = new LinkedHashMap<>();
    document.put("uri", uri);
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("textDocument", document);
    return client
        .request("textDocument/documentSymbol", params)
        .thenApply(LanguageSession::symbolsOf);
  }

''')

before(LSP, '  static List<Diagnostic> diagnosticsOf(Object list) {', r'''  /**
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

''')

# ---- editor: the server client ----
replace(SERVER, 'import org.codirex.asm.domain.lsp.Diagnostic;',
        'import org.codirex.asm.domain.lsp.Diagnostic;\nimport org.codirex.asm.domain.lsp.DocumentSymbol;')
before(SERVER, '  // ---- inside ----', r'''  /** The symbols of the document in the order they are declared; null if the server did not answer. */
  List<DocumentSymbol> documentSymbols(String uri, String text) {
    return ask(uri, text, s -> s.documentSymbols(uri));
  }

''')

# ---- editor: the fragment ----
replace(EDITOR, 'import org.codirex.asm.domain.lsp.Diagnostic;',
        'import org.codirex.asm.domain.lsp.Diagnostic;\nimport org.codirex.asm.domain.lsp.DocumentSymbol;')
before(EDITOR, '  private void say(int message) {', r'''  /**
   * Asks the language server for the symbols of the shown file. {@code done} is called on the main
   * thread with the symbols (there may be none), or with null if the file has no server that is
   * ready.
   */
  public void loadSymbols(java.util.function.Consumer<List<DocumentSymbol>> done) {
    final String path = currentPath;
    final JavaLanguageServer server = serverFor(path);
    if (binding == null
        || server == null
        || server.state() != JavaLanguageServer.State.READY) {
      done.accept(null);
      return;
    }
    final String text = binding.editor.getText().toString();
    final String uri = uriOf(path);
    new Thread(
            () -> {
              List<DocumentSymbol> found = server.documentSymbols(uri, text);
              handler.post(() -> done.accept(found));
            },
            "java-symbols")
        .start();
  }

  /** Puts the caret at a position (zero based) of the shown file. */
  public void goTo(int line, int column) {
    if (binding == null) {
      return;
    }
    io.github.rosemoe.sora.text.Content content = binding.editor.getText();
    int l = Math.max(0, Math.min(line, content.getLineCount() - 1));
    int c = Math.max(0, Math.min(column, content.getColumnCount(l)));
    binding.editor.setSelection(l, c);
  }

''')

# ---- workspace ----
before(APP + "WorkspaceFragment.java", '  /** Formats the shown file. */', r'''  /** Asks the language server of the shown file for its symbols (null: not available). */
  void symbols(
      java.util.function.Consumer<java.util.List<org.codirex.asm.domain.lsp.DocumentSymbol>> done) {
    EditorFragment editor = editor();
    if (editor == null) {
      done.accept(null);
    } else {
      editor.loadSymbols(done);
    }
  }

  /** Puts the caret at a position (zero based) of the shown file. */
  void goTo(int line, int column) {
    EditorFragment editor = editor();
    if (editor != null) {
      editor.goTo(line, column);
    }
  }

''')

# ---- activity ----
before(APP + "MainActivity.java", '  @Override\n  public File paletteProjectDirectory() {', r'''  @Override
  public void paletteSymbols(
      java.util.function.Consumer<java.util.List<org.codirex.asm.domain.lsp.DocumentSymbol>> done) {
    WorkspaceFragment workspace = workspace();
    if (workspace == null) {
      done.accept(null);
    } else {
      workspace.symbols(done);
    }
  }

  @Override
  public void paletteGoTo(int line, int column) {
    WorkspaceFragment workspace = workspace();
    if (workspace != null) {
      workspace.goTo(line, column);
    }
  }

''')

# ---- palette ----
PAL = APP + "CommandPaletteFragment.java"
replace(PAL, 'import org.codirex.asm.domain.palette.FileIndex;',
        'import org.codirex.asm.domain.lsp.DocumentSymbol;\nimport org.codirex.asm.domain.palette.FileIndex;')
replace(PAL, '    void paletteOpenFile(String relativePath);',
        r'''    void paletteOpenFile(String relativePath);

    /** The symbols of the shown file, on the main thread; null if there is no server for it. */
    void paletteSymbols(java.util.function.Consumer<List<DocumentSymbol>> done);

    /** Puts the caret at a position (zero based) of the shown file. */
    void paletteGoTo(int line, int column);''')
replace(PAL, '  private List<PaletteItem> fileItems = Collections.emptyList();',
        r'''  private List<PaletteItem> fileItems = Collections.emptyList();
  private List<PaletteItem> symbolItems;
  private boolean symbolsRequested;
  private boolean symbolsFailed;''')
replace(PAL, '    if (query.startsWith(">")) {',
        r'''    if (query.startsWith("@")) {
      requestSymbols();
      List<PaletteItem> source =
          symbolItems == null ? Collections.<PaletteItem>emptyList() : symbolItems;
      shown = FuzzyMatcher.rank(query.substring(1).trim(), source, item -> item.matchText, MAX_RESULTS);
    } else if (query.startsWith(">")) {''')
replace(PAL, '    binding.paletteEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);',
        r'''    int emptyText = R.string.palette_empty;
    if (query.startsWith("@") && symbolItems == null) {
      emptyText =
          symbolsFailed ? R.string.palette_symbols_unavailable : R.string.palette_symbols_loading;
    }
    binding.paletteEmpty.setText(emptyText);
    binding.paletteEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);''')
before(PAL, '  /** The rows of the palette. */', r'''  /** Asks once for the symbols of the shown file; the palette fills in when they arrive. */
  private void requestSymbols() {
    if (symbolsRequested) {
      return;
    }
    symbolsRequested = true;
    final Host host = (Host) requireActivity();
    host.paletteSymbols(
        found -> {
          if (binding == null) {
            return;
          }
          if (found == null) {
            symbolsFailed = true;
          } else {
            List<PaletteItem> built = new ArrayList<>(found.size());
            for (DocumentSymbol symbol : found) {
              final int line = symbol.line();
              final int column = symbol.character();
              String subtitle = symbol.kindName();
              if (!symbol.container().isEmpty()) {
                subtitle += " - " + symbol.container();
              }
              built.add(
                  PaletteItem.file(
                      symbol.name(),
                      subtitle,
                      symbol.name(),
                      R.drawable.ic_palette_symbol,
                      () -> host.paletteGoTo(line, column)));
            }
            symbolItems = built;
          }
          refresh();
        });
  }

''')

replace("docs/adr/0035-command-palette.md",
        "Symbol search (`@`) comes with the language server's workspace symbols.",
        "A leading `@` searches the symbols of the shown file (the server's `textDocument/documentSymbol`, read by `LanguageSession`); choosing one puts the caret on it. Symbols of the whole project (`workspace/symbol`) are a later step.")
print("done")
