"""One-off: a language server per language (Java -> jdtls, XML -> lemminx)."""
import pathlib


def patch(path, pairs):
    p = pathlib.Path(path)
    s = p.read_text(encoding="utf-8")
    for item in pairs:
        old, new = item[0], item[1]
        n = item[2] if len(item) > 2 else 1
        assert s.count(old) == n, (path, old[:80], s.count(old))
        s = s.replace(old, new)
    p.write_text(s, encoding="utf-8")


EDITOR = "feature/editor/src/main/java/org/codirex/asm/feature/editor/"

# ---- terminal service: the launcher knows lemminx ----
patch("terminal/service/src/main/java/org/codirex/asm/terminal/service/LanguageServerLauncher.java", [
    (r'''  /** The command for a language and a workspace key; null for a language without a server. */
''', r'''  /** The program of a language's server, or null for a language without one. */
  private static String programOf(String language) {
    if ("java".equals(language)) {
      return "jdtls";
    }
    if ("xml".equals(language)) {
      return "lemminx";
    }
    return null;
  }

  /** The command for a language and a workspace key; null for a language without a server. */
'''),
    (r'''    if (!"java".equals(language)) {
      return null;
    }
    if (!isValidKey(workspaceKey)) {''', r'''    String program = programOf(language);
    if (program == null) {
      return null;
    }
    if (!isValidKey(workspaceKey)) {'''),
    (r'''    File script = new File(prefix, "bin/jdtls");''', r'''    File script = new File(prefix, "bin/" + program);'''),
    (r'''    File data = new File(environment.home(), ".cache/jdtls/workspace-" + workspaceKey);
''', ''),
    (r'''    command.add(data.getPath());
''', r'''    if ("java".equals(language)) {
      // jdt.ls keeps its data per workspace; lemminx keeps none.
      command.add(new File(environment.home(), ".cache/jdtls/workspace-" + workspaceKey).getPath());
    }
'''),
    (r'''    File logs = new File(home, ".cache/jdtls");''', r'''    File logs = new File(home, ".cache/" + programOf(language));'''),
])

# ---- editor: one server object per language ----
patch(EDITOR + "JavaLanguageServer.java", [
    (" * The Java language server of one project, as the editor uses it (ADR-0031): it connects to the",
     " * The language server of one language of one project (ADR-0031, ADR-0033: jdt.ls for Java, LemMinX for XML), as the editor uses it: it connects to the"),
    ('  private static final String LANGUAGE = "java";\n', ''),
    ("  private final String workspaceKey;\n", "  private final String workspaceKey;\n  private final String language;\n"),
    (r'''  JavaLanguageServer(Context context, File projectDirectory, String workspaceKey) {
    this.context = context.getApplicationContext();
    this.rootUri = uriOf(projectDirectory);
    this.workspaceKey = workspaceKey;
''', r'''  /** @param language the LSP language id the service knows: {@code java} or {@code xml} */
  JavaLanguageServer(
      Context context, File projectDirectory, String workspaceKey, String language) {
    this.context = context.getApplicationContext();
    this.rootUri = uriOf(projectDirectory);
    this.workspaceKey = workspaceKey;
    this.language = language;
'''),
    ("service.openLanguageServer(LANGUAGE, workspaceKey)", "service.openLanguageServer(language, workspaceKey)"),
    ("            initializationOptions(),\n", '            "java".equals(language) ? initializationOptions() : null,\n'),
    ("current.open(uri, LANGUAGE, 1, text)", "current.open(uri, language, 1, text)"),
])

pathlib.Path(EDITOR + "LanguageServerHolder.java").write_text(r'''package org.codirex.asm.feature.editor;

import androidx.lifecycle.ViewModel;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps the language servers of the open project (one per language) across a change of screen
 * configuration, so that turning the phone does not restart them. They are stopped when the editor
 * goes away for good.
 */
public final class LanguageServerHolder extends ViewModel {

  private final Map<String, JavaLanguageServer> servers = new HashMap<>();

  /** The server of a language, or null if none was made yet. */
  JavaLanguageServer server(String language) {
    return servers.get(language);
  }

  void setServer(String language, JavaLanguageServer server) {
    servers.put(language, server);
  }

  @Override
  protected void onCleared() {
    for (JavaLanguageServer server : servers.values()) {
      server.stop();
    }
    servers.clear();
  }
}
''', encoding="utf-8")

SETUP_OLD = r'''    if (holder.java() == null) {
      holder.setJava(
          new JavaLanguageServer(
              requireContext(),
              projectDirectory,
              workspaceKey(requireArguments().getString(EditorViewModel.ARG_PROJECT))));
    }
    javaServer = holder.java();
    // The server outlives the view; its reports go to the view that is there now.
    javaServer.setListener(
        new JavaLanguageServer.Listener() {
          @Override
          public void onState(JavaLanguageServer.State state) {
            handler.post(() -> onServerState(state));
          }

          @Override
          public void onDiagnostics(String uri, List<Diagnostic> found) {
            handler.post(() -> onServerDiagnostics(uri, found));
          }
        });
'''
SETUP_NEW = r'''    servers.clear();
    for (final String language : LSP_LANGUAGES) {
      JavaLanguageServer server = holder.server(language);
      if (server == null) {
        server =
            new JavaLanguageServer(
                requireContext(),
                projectDirectory,
                workspaceKey(requireArguments().getString(EditorViewModel.ARG_PROJECT)),
                language);
        holder.setServer(language, server);
      }
      servers.put(language, server);
      // The server outlives the view; its reports go to the view that is there now.
      server.setListener(
          new JavaLanguageServer.Listener() {
            @Override
            public void onState(JavaLanguageServer.State state) {
              handler.post(() -> onServerState(language, state));
            }

            @Override
            public void onDiagnostics(String uri, List<Diagnostic> found) {
              handler.post(() -> onServerDiagnostics(uri, found));
            }
          });
    }
'''

ISJAVA_OLD = r'''  private static boolean isJava(@Nullable String path) {
    return path != null && SourceLanguage.forPath(path) == SourceLanguage.JAVA;
  }
'''
ISJAVA_NEW = r'''  /** The language id of the server for a file, or null if its language has none. */
  @Nullable
  private static String lspLanguage(@Nullable String path) {
    if (path == null) {
      return null;
    }
    SourceLanguage language = SourceLanguage.forPath(path);
    if (language == SourceLanguage.JAVA) {
      return "java";
    }
    return language == SourceLanguage.XML ? "xml" : null;
  }

  /** The server for the language of a file, or null. */
  @Nullable
  private JavaLanguageServer serverFor(@Nullable String path) {
    String language = lspLanguage(path);
    return language == null ? null : servers.get(language);
  }
'''

INSTALL_OLD = r'''    if (state == JavaLanguageServer.State.MISSING && !installOffered) {
      installOffered = true;
      Snackbar.make(binding.getRoot(), R.string.editor_java_server_missing, Snackbar.LENGTH_INDEFINITE)
          .setAction(
              R.string.editor_java_server_install,
              v -> ((EditorNavigator) requireActivity()).installPackage("jdtls"))
          .show();
    }
'''
INSTALL_NEW = r'''    if (state == JavaLanguageServer.State.MISSING && installOffered.add(language)) {
      final String pack = "xml".equals(language) ? "lemminx" : "jdtls";
      Snackbar.make(
              binding.getRoot(),
              "xml".equals(language)
                  ? R.string.editor_xml_server_missing
                  : R.string.editor_java_server_missing,
              Snackbar.LENGTH_INDEFINITE)
          .setAction(
              R.string.editor_java_server_install,
              v -> ((EditorNavigator) requireActivity()).installPackage(pack))
          .show();
    }
'''

GUARD_OLD = r'''    final JavaLanguageServer server = javaServer;
    if (binding == null || server == null || !isJava(path)) {
'''
GUARD_NEW = r'''    final JavaLanguageServer server = serverFor(path);
    if (binding == null || server == null) {
'''

RENDER_OLD = r'''      if (isJava(state.shownPath()) && javaServer != null) {
        javaServer.start();
        javaServer.documentChanged(uriOf(state.shownPath()), state.shownText().toString());
      }
'''
RENDER_NEW = r'''      JavaLanguageServer shownServer = serverFor(state.shownPath());
      if (shownServer != null) {
        shownServer.start();
        shownServer.documentChanged(uriOf(state.shownPath()), state.shownText().toString());
      }
'''

TABS_OLD = r'''    if (javaServer != null) {
      for (String old : renderedPaths) {
        if (!paths.contains(old) && isJava(old)) {
          javaServer.documentClosed(uriOf(old));
        }
      }
    }
'''
TABS_NEW = r'''    for (String old : renderedPaths) {
      JavaLanguageServer closing = serverFor(old);
      if (closing != null && !paths.contains(old)) {
        closing.documentClosed(uriOf(old));
      }
    }
'''

patch(EDITOR + "EditorFragment.java", [
    ("import java.util.Collections;\nimport java.util.List;\nimport java.util.Set;\n",
     "import java.util.Collections;\nimport java.util.HashMap;\nimport java.util.HashSet;\nimport java.util.List;\nimport java.util.Map;\nimport java.util.Set;\n"),
    ("  private static final long SERVER_SYNC_DELAY_MS = 400;\n",
     '  private static final long SERVER_SYNC_DELAY_MS = 400;\n\n  /** The languages that have a language server (ADR-0031, ADR-0033). */\n  private static final String[] LSP_LANGUAGES = {"java", "xml"};\n'),
    ("  /** The Java language server of the project (ADR-0031), or null before the view exists. */\n  private JavaLanguageServer javaServer;\n",
     "  /** The language servers of the project by language id (ADR-0031), empty before the view exists. */\n  private final Map<String, JavaLanguageServer> servers = new HashMap<>();\n"),
    ("  private boolean installOffered;\n", "  private final Set<String> installOffered = new HashSet<>();\n"),
    (SETUP_OLD, SETUP_NEW),
    ("    if (javaServer != null) {\n      javaServer.setListener(null);\n    }\n",
     "    for (JavaLanguageServer server : servers.values()) {\n      server.setListener(null);\n    }\n"),
    ("    if (isJava(currentPath)) {\n      handler.removeCallbacks(syncServer);",
     "    if (serverFor(currentPath) != null) {\n      handler.removeCallbacks(syncServer);"),
    (ISJAVA_OLD, ISJAVA_NEW),
    (r'''    String path = currentPath;\n    if (binding != null && javaServer != null && isJava(path)) {\n      javaServer.documentChanged(uriOf(path), binding.editor.getText().toString());\n    }\n''',
     r'''    String path = currentPath;\n    JavaLanguageServer server = serverFor(path);\n    if (binding != null && server != null) {\n      server.documentChanged(uriOf(path), binding.editor.getText().toString());\n    }\n'''),
    ("  private void onServerState(JavaLanguageServer.State state) {", "  private void onServerState(String language, JavaLanguageServer.State state) {"),
    (INSTALL_OLD, INSTALL_NEW),
    (r'''    return javaServer != null\n        && javaServer.state() == JavaLanguageServer.State.READY\n        && isJava(currentPath);\n''',
     r'''    JavaLanguageServer server = serverFor(currentPath);\n    return server != null && server.state() == JavaLanguageServer.State.READY;\n'''),
    (GUARD_OLD, GUARD_NEW, 3),
    (RENDER_OLD, RENDER_NEW),
    ("    if (base == null || language != SourceLanguage.JAVA) {",
     "    if (base == null || (language != SourceLanguage.JAVA && language != SourceLanguage.XML)) {"),
    ("          JavaLanguageServer server = javaServer;\n          if (path == null || server == null || !isJava(path)) {",
     "          JavaLanguageServer server = serverFor(path);\n          if (path == null || server == null) {"),
    (TABS_OLD, TABS_NEW),
    ("    if (isJava(currentPath) && javaServer != null) {\n      int label;\n      switch (javaServer.state()) {",
     "    JavaLanguageServer fileServer = serverFor(currentPath);\n    if (fileServer != null) {\n      int label;\n      switch (fileServer.state()) {"),
    ("      binding.statusServer.setText(label);\n",
     '      binding.statusServer.setText(\n          getString(label, "xml".equals(lspLanguage(currentPath)) ? "XML" : "Java"));\n'),
])

# ---- strings: the status texts name the language; the XML install hint ----
XML_HINT = {
    "values": '    <string name="editor_xml_server_missing">The XML language server is not installed. It gives error marks and completion for XML.</string>\n',
    "values-ar": '    <string name="editor_xml_server_missing">خادم لغة XML غير مثبّت. يوفّر علامات الأخطاء والإكمال لملفات XML.</string>\n',
}
for res, hint in XML_HINT.items():
    p = pathlib.Path("feature/editor/src/main/res/%s/strings.xml" % res)
    lines = p.read_text(encoding="utf-8").split("\n")
    lines = [l.replace("Java", "%1$s") if 'name="status_java_' in l else l for l in lines]
    text = "\n".join(lines)
    assert text.count("</resources>") == 1
    p.write_text(text.replace("</resources>", hint + "</resources>"), encoding="utf-8")
print("done")
