"""One-off: connects domain:xmlassist to the build and to the editor's completion."""
import pathlib


def replace(path, old, new):
    p = pathlib.Path(path)
    s = p.read_text(encoding="utf-8")
    assert s.count(old) == 1, (path, old[:80], s.count(old))
    p.write_text(s.replace(old, new), encoding="utf-8")


EDITOR = "feature/editor/src/main/java/org/codirex/asm/feature/editor/EditorFragment.java"
LANG = "feature/editor/src/main/java/org/codirex/asm/feature/editor/ServerLanguage.java"

replace("settings.gradle.kts", 'include(":domain:palette")',
        'include(":domain:palette")\ninclude(":domain:xmlassist")')
replace("feature/editor/build.gradle.kts", '    implementation(project(":domain:lsp"))',
        '    implementation(project(":domain:lsp"))\n    implementation(project(":domain:xmlassist"))')

replace(LANG,
        '''      publisher.addItem(
          new SimpleCompletionItem(item.label(), item.detail(), replace, item.insertText()));''',
        '''      publisher.addItem(
          new CaretCompletionItem(item.label(), item.detail(), replace, item.insertText()));''')
replace(LANG, 'import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem;\n', '')

replace(EDITOR, 'import org.codirex.asm.domain.project.ProjectName;',
        'import org.codirex.asm.domain.project.ProjectName;\nimport org.codirex.asm.domain.xmlassist.AndroidXmlCompleter;\nimport org.codirex.asm.domain.xmlassist.AttrCatalog;')

replace(EDITOR,
        '''    return new ServerLanguage(
        base,
        (text, line, column) -> {
          String path = currentPath;
          JavaLanguageServer server = serverFor(path);
          if (path == null || server == null) {
            return null;
          }
          return server.complete(uriOf(path), text, line, column);
        });''',
        '''    final File filesDirectory = requireContext().getFilesDir();
    return new ServerLanguage(base, (text, line, column) -> completeAt(filesDirectory, text, line, column));''')

replace(EDITOR, '  private void renderTabs(EditorState state) {',
        '''  /**
   * The proposals of the shown file at a position: the language server's, and for a layout file the
   * Android attributes before them (ADR-0036). Null when nothing can say, so the editor falls back
   * to its own.
   */
  @Nullable
  private List<org.codirex.asm.domain.lsp.CompletionItem> completeAt(
      File filesDirectory, String text, int line, int column) {
    String path = currentPath;
    JavaLanguageServer server = serverFor(path);
    if (path == null || server == null) {
      return null;
    }
    List<org.codirex.asm.domain.lsp.CompletionItem> fromServer =
        server.complete(uriOf(path), text, line, column);
    if (!AndroidXmlCompleter.appliesTo(path)) {
      return fromServer;
    }
    AttrCatalog catalog = AndroidAttrs.get(filesDirectory);
    if (catalog == null) {
      return fromServer;
    }
    List<org.codirex.asm.domain.lsp.CompletionItem> merged = new ArrayList<>();
    Set<String> labels = new HashSet<>();
    int offset = AndroidXmlCompleter.offsetOf(text, line, column);
    for (AndroidXmlCompleter.Proposal p : new AndroidXmlCompleter(catalog).complete(text, offset)) {
      labels.add(p.label());
      merged.add(
          new org.codirex.asm.domain.lsp.CompletionItem(
              p.label(),
              p.detail(),
              10,
              p.insertText(),
              line,
              column - p.replaceLength(),
              line,
              column));
    }
    if (fromServer != null) {
      for (org.codirex.asm.domain.lsp.CompletionItem item : fromServer) {
        if (!labels.contains(item.label())) {
          merged.add(item);
        }
      }
    }
    return merged.isEmpty() ? fromServer : merged;
  }

  private void renderTabs(EditorState state) {''')
print("done")
