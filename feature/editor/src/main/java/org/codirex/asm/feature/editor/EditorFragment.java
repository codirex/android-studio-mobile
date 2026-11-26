package org.codirex.asm.feature.editor;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.event.ScrollEvent;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticProvider;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow;
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.codirex.asm.domain.editor.SourceLanguage;
import org.codirex.asm.domain.lsp.Diagnostic;
import org.codirex.asm.domain.lsp.DocumentSymbol;
import org.codirex.asm.domain.lsp.Location;
import org.codirex.asm.domain.lsp.TextEdit;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.feature.editor.databinding.FragmentEditorBinding;
import org.codirex.asm.feature.editor.databinding.ItemEditorTabBinding;

/**
 * The editor with its tabs. The host calls {@link #open} when the user picks a file. The editor
 * widget shows one document at a time; its text is handed to the view model before every change of
 * tab and when the screen pauses, and saved one second after the last edit.
 */
@AndroidEntryPoint
public final class EditorFragment extends Fragment {

  private static final long AUTO_SAVE_DELAY_MS = 1000;
  private static final long SERVER_SYNC_DELAY_MS = 400;

  /** The languages that have a language server (ADR-0031, ADR-0033). */
  private static final String[] LSP_LANGUAGES = {"java", "xml"};

  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable autoSave = this::snapshotCurrent;

  private EditorViewModel viewModel;
  private FragmentEditorBinding binding;

  /** The document whose text the editor widget holds, or null. */
  private volatile String currentPath;

  /** The language servers of the project by language id (ADR-0031), empty before the view exists. */
  private final Map<String, JavaLanguageServer> servers = new HashMap<>();

  /** The menu above a selection (copy, paste, definition...), or null before the view exists. */
  private SelectionMenu selectionMenu;

  private File projectDirectory;
  private final Set<String> installOffered = new HashSet<>();

  /** The diagnostics of the shown document, as the server last said (main thread only). */
  private List<Diagnostic> diagnostics = Collections.emptyList();

  private String diagnosticsUri;
  private final Runnable syncServer = this::syncServerNow;
  private final DiagnosticProvider diagnosticProvider = this::provideDiagnostics;

  private int appliedRevision = -1;
  private SourceLanguage appliedLanguage = SourceLanguage.PLAIN;
  private List<String> renderedPaths = Collections.emptyList();
  private Set<String> renderedDirty = Collections.emptySet();
  private boolean rendering;

  /** A line to put the cursor on once {@link #pendingPath} is shown (1-based), or 0. */
  private int pendingLine;

  private String pendingPath;

  /** Creates the editor for {@code project}. */
  public static EditorFragment newInstance(ProjectName project) {
    Bundle arguments = new Bundle();
    arguments.putString(EditorViewModel.ARG_PROJECT, project.value());
    EditorFragment fragment = new EditorFragment();
    fragment.setArguments(arguments);
    return fragment;
  }

  /** Opens a file of the project in a tab and shows it. */
  public void open(String relativePath) {
    if (viewModel == null) {
      return;
    }
    snapshotCurrent();
    viewModel.open(relativePath);
  }

  /**
   * Opens a file and puts the cursor on {@code line} (1-based) once its text is shown. A line below
   * 1 only opens the file.
   */
  public void open(String relativePath, int line) {
    if (viewModel == null) {
      return;
    }
    pendingPath = line > 0 ? relativePath : null;
    pendingLine = line;
    open(relativePath);
    // A file that is already shown changes no state, so nothing would render: go to the line now.
    applyPendingLine();
  }

  /** Moves the cursor to the pending line if the file it is for is the one the widget holds. */
  private void applyPendingLine() {
    if (binding == null || pendingPath == null || !pendingPath.equals(currentPath)) {
      return;
    }
    int lines = binding.editor.getText().getLineCount();
    int line = Math.max(0, Math.min(pendingLine - 1, lines - 1));
    pendingPath = null;
    pendingLine = 0;
    binding.editor.setSelection(line, 0);
  }

  /**
   * A file or folder was deleted: closes its tab, and the tabs of the files inside it, without
   * saving them.
   */
  public void closeUnder(String relativePath) {
    if (viewModel == null) {
      return;
    }
    handler.removeCallbacks(autoSave);
    viewModel.discardUnder(relativePath);
    // The pending save of the tab that stays was cancelled above; do it now.
    snapshotCurrent();
  }

  /**
   * A file or folder was renamed: its tab, and the tabs of the files inside it, follow it. Unsaved
   * text is then saved under the new name, not the old one.
   */
  public void renameUnder(String from, String to) {
    if (viewModel == null) {
      return;
    }
    handler.removeCallbacks(autoSave);
    viewModel.renameUnder(from, to);
    snapshotCurrent();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentEditorBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(this).get(EditorViewModel.class);
    projectDirectory =
        new File(requireContext().getFilesDir(), "projects/" + requireArguments().getString(EditorViewModel.ARG_PROJECT));
    LanguageServerHolder holder = new ViewModelProvider(this).get(LanguageServerHolder.class);
    servers.clear();
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

    // A new view starts empty, whatever the previous one showed. A new editor widget has no
    // language, which behaves as plain text.
    currentPath = null;
    appliedRevision = -1;
    appliedLanguage = SourceLanguage.PLAIN;
    renderedPaths = Collections.emptyList();
    renderedDirty = Collections.emptySet();

    boolean night =
        (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
    // The TextMate colour scheme follows the theme of the TextMate registry (ADR-0026); without
    // TextMate the editor keeps the built-in schemes and shows plain text.
    if (TextMateSetup.ensure(requireContext())) {
      TextMateSetup.useTheme(night);
      binding.editor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));
    } else {
      binding.editor.setColorScheme(night ? new SchemeDarcula() : new SchemeGitHub());
    }
    binding.editor.subscribeAlways(ContentChangeEvent.class, this::onContentChanged);
    binding.editor.registerDiagnosticProvider(diagnosticProvider);
    android.graphics.Typeface mono =
        androidx.core.content.res.ResourcesCompat.getFont(
            requireContext(), org.codirex.asm.core.ui.R.font.jetbrains_mono_regular);
    if (mono != null) {
      binding.editor.setTypefaceText(mono);
    }

    binding.showFiles.setOnClickListener(
        v -> ((EditorNavigator) requireActivity()).showFileTree());
    setUpSelectionMenu();
    setUpStatusLine();
    setUpQuickKeys();
    binding.tabs.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(TabLayout.Tab tab) {
            if (rendering || !(tab.getTag() instanceof String)) {
              return;
            }
            snapshotCurrent();
            viewModel.select((String) tab.getTag());
          }

          @Override
          public void onTabUnselected(TabLayout.Tab tab) {}

          @Override
          public void onTabReselected(TabLayout.Tab tab) {}
        });

    viewModel.state().observe(getViewLifecycleOwner(), this::render);
  }

  @Override
  public void onPause() {
    snapshotCurrent();
    super.onPause();
  }

  @Override
  public void onDestroyView() {
    handler.removeCallbacks(autoSave);
    handler.removeCallbacks(syncServer);
    for (JavaLanguageServer server : servers.values()) {
      server.setListener(null);
    }
    binding.editor.unregisterDiagnosticProvider(diagnosticProvider);
    if (selectionMenu != null) {
      selectionMenu.dismiss();
      selectionMenu = null;
    }
    binding.editor.release();
    binding = null;
    super.onDestroyView();
  }

  private void onContentChanged(ContentChangeEvent event) {
    // Setting the text from the view model is not an edit.
    if (event.getAction() == ContentChangeEvent.ACTION_SET_NEW_TEXT || currentPath == null) {
      return;
    }
    viewModel.onEdited(currentPath);
    handler.removeCallbacks(autoSave);
    handler.postDelayed(autoSave, AUTO_SAVE_DELAY_MS);
    if (serverFor(currentPath) != null) {
      handler.removeCallbacks(syncServer);
      handler.postDelayed(syncServer, SERVER_SYNC_DELAY_MS);
    }
  }

  /** The language id of the server for a file, or null if its language has none. */
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

  private String uriOf(String relativePath) {
    return JavaLanguageServer.uriOf(new File(projectDirectory, relativePath));
  }

  /** A name for the workspace data of jdt.ls: letters, digits, dot, dash and underscore. */
  private static String workspaceKey(String project) {
    return project.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  /** Sends the live text of the shown Java file to the server (a moment after the last edit). */
  private void syncServerNow() {
    String path = currentPath;
    JavaLanguageServer server = serverFor(path);
    if (binding != null && server != null) {
      server.documentChanged(uriOf(path), binding.editor.getText().toString());
    }
  }

  private void onServerState(String language, JavaLanguageServer.State state) {
    if (binding == null) {
      return;
    }
    updateStatus();
    if (state == JavaLanguageServer.State.MISSING && installOffered.add(language)) {
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
    if (state == JavaLanguageServer.State.READY) {
      syncServerNow();
    }
  }

  private void onServerDiagnostics(String uri, List<Diagnostic> found) {
    if (binding == null || currentPath == null || !uri.equals(uriOf(currentPath))) {
      return;
    }
    diagnosticsUri = uri;
    diagnostics = found;
    binding.editor.invalidateDiagnostics();
  }

  /**
   * Gives the editor the squiggles for the diagnostics of the shown file, each with the server's
   * message for the editor's tooltip.
   */
  private void provideDiagnostics(DiagnosticsContainer container) {
    if (binding == null || diagnostics.isEmpty() || currentPath == null) {
      return;
    }
    if (!uriOf(currentPath).equals(diagnosticsUri)) {
      return;
    }
    io.github.rosemoe.sora.text.Content text = binding.editor.getText();
    List<DiagnosticRegion> regions = new ArrayList<>();
    for (Diagnostic d : diagnostics) {
      if (d.startLine() >= text.getLineCount()) {
        continue;
      }
      int endLine = Math.min(d.endLine(), text.getLineCount() - 1);
      int start = text.getCharIndex(d.startLine(), Math.min(d.startCharacter(), text.getColumnCount(d.startLine())));
      int end = text.getCharIndex(endLine, Math.min(d.endCharacter(), text.getColumnCount(endLine)));
      if (end <= start) {
        end = Math.min(start + 1, text.length());
      }
      short severity =
          d.severity() == Diagnostic.ERROR
              ? DiagnosticRegion.SEVERITY_ERROR
              : d.severity() == Diagnostic.WARNING
                  ? DiagnosticRegion.SEVERITY_WARNING
                  : DiagnosticRegion.SEVERITY_TYPO;
      DiagnosticDetail detail = new DiagnosticDetail(d.message(), null, null, null);
      regions.add(new DiagnosticRegion(start, end, severity, regions.size(), detail));
    }
    container.addDiagnostics(regions);
  }

  /** Whether the language server of the shown file can answer (Definition, Info, Format). */
  private boolean codeActionsAvailable() {
    JavaLanguageServer server = serverFor(currentPath);
    return server != null && server.state() == JavaLanguageServer.State.READY;
  }

  /**
   * Replaces the editor's own selection window with {@link SelectionMenu}: the usual text actions
   * and, when the server of the file is ready, Definition, Info and Format. It appears after a long
   * press that selects text, and goes away when the selection ends, the text changes or the editor
   * scrolls.
   */
  private void setUpSelectionMenu() {
    binding.editor.getComponent(EditorTextActionWindow.class).setEnabled(false);
    selectionMenu =
        new SelectionMenu(
            requireContext(),
            new SelectionMenu.Actions() {
              @Override
              public void cut() {
                if (binding != null) {
                  binding.editor.cutText();
                }
              }

              @Override
              public void copy() {
                if (binding != null) {
                  binding.editor.copyText();
                }
              }

              @Override
              public void paste() {
                if (binding != null) {
                  binding.editor.pasteText();
                }
              }

              @Override
              public void selectAll() {
                if (binding != null) {
                  binding.editor.selectAll();
                }
              }

              @Override
              public void definition() {
                goToDefinition();
              }

              @Override
              public void info() {
                showHover();
              }

              @Override
              public void format() {
                formatDocument();
              }
            });
    binding.editor.subscribeAlways(SelectionChangeEvent.class, this::onSelectionChanged);
    binding.editor.subscribeAlways(ScrollEvent.class, event -> hideSelectionMenu());
  }

  private void hideSelectionMenu() {
    if (selectionMenu != null) {
      selectionMenu.dismiss();
    }
  }

  private void onSelectionChanged(SelectionChangeEvent event) {
    if (binding == null || selectionMenu == null) {
      return;
    }
    if (!binding.editor.getCursor().isSelected()) {
      hideSelectionMenu();
      return;
    }
    // Only a long press (or dragging the handles after one) opens the menu; code and search that
    // move the selection do not.
    if (event.getCause() == SelectionChangeEvent.CAUSE_LONG_PRESS) {
      selectionMenu.show(
          binding.editor,
          binding.editor.getCursor().getLeftLine(),
          binding.editor.getCursor().getLeftColumn(),
          codeActionsAvailable());
    }
  }

  /** Undo/redo buttons, and the language server state, caret position and encoding at the bottom. */
  private void setUpStatusLine() {
    binding.undo.setOnClickListener(
        v -> {
          if (binding != null) {
            binding.editor.undo();
          }
        });
    binding.redo.setOnClickListener(
        v -> {
          if (binding != null) {
            binding.editor.redo();
          }
        });
    binding.editor.subscribeAlways(SelectionChangeEvent.class, event -> updateStatus());
    binding.editor.subscribeAlways(ContentChangeEvent.class, event -> updateUndoRedo());
    updateStatus();
    updateUndoRedo();
  }

  /** Shows the bar only while a file is shown, with the caret position and the server state. */
  private void updateStatus() {
    if (binding == null) {
      return;
    }
    boolean shown = currentPath != null;
    int visibility = shown ? View.VISIBLE : View.GONE;
    binding.statusBar.setVisibility(visibility);
    binding.statusDivider.setVisibility(visibility);
    binding.quickKeysScroll.setVisibility(visibility);
    binding.breadcrumb.setVisibility(visibility);
    binding.breadcrumb.setText(shown ? currentPath.replace("/", "  \u203A  ") : "");
    if (!shown) {
      return;
    }
    binding.statusPosition.setText(
        getString(
            R.string.status_position,
            binding.editor.getCursor().getLeftLine() + 1,
            binding.editor.getCursor().getLeftColumn() + 1));
    JavaLanguageServer fileServer = serverFor(currentPath);
    if (fileServer != null) {
      int label;
      switch (fileServer.state()) {
        case READY:
          label = R.string.status_java_ready;
          break;
        case MISSING:
          label = R.string.status_java_missing;
          break;
        case FAILED:
          label = R.string.status_java_failed;
          break;
        case STARTING:
          label = R.string.status_java_starting;
          break;
        default:
          label = R.string.status_java_off;
          break;
      }
      binding.statusServer.setText(
          getString(label, "xml".equals(lspLanguage(currentPath)) ? "XML" : "Java"));
    } else {
      binding.statusServer.setText("");
    }
  }

  private void updateUndoRedo() {
    if (binding == null) {
      return;
    }
    boolean canUndo = currentPath != null && binding.editor.canUndo();
    boolean canRedo = currentPath != null && binding.editor.canRedo();
    binding.undo.setEnabled(canUndo);
    binding.undo.setAlpha(canUndo ? 1f : 0.38f);
    binding.redo.setEnabled(canRedo);
    binding.redo.setAlpha(canRedo ? 1f : 0.38f);
  }

  /** The symbols that are slow to reach on a phone keyboard; Tab inserts four spaces. */
  private void setUpQuickKeys() {
    String[] keys = {
      "Tab", "{", "}", "(", ")", "[", "]", ";", "\"", "'", "=", "<", ">", "/", "!", ".", ","
    };
    LayoutInflater inflater = getLayoutInflater();
    for (String key : keys) {
      android.widget.TextView view =
          (android.widget.TextView)
              inflater.inflate(R.layout.item_quick_key, binding.quickKeys, false);
      view.setText(key);
      final String text = key.equals("Tab") ? "    " : key;
      view.setOnClickListener(
          v -> {
            if (binding != null && currentPath != null) {
              binding.editor.insertText(text, text.length());
            }
          });
      binding.quickKeys.addView(view);
    }
  }

  /** Format the shown file (the palette and the selection menu share this). */
  public void runFormat() {
    formatDocument();
  }

  /** Go to the definition of the symbol at the caret. */
  public void runDefinition() {
    goToDefinition();
  }

  /** Show what the language server says about the symbol at the caret. */
  public void runInfo() {
    showHover();
  }

  /**
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

  private void say(int message) {
    if (binding != null) {
      Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
  }

  /** Goes to the definition of the symbol at the caret. */
  private void goToDefinition() {
    final String path = currentPath;
    final JavaLanguageServer server = serverFor(path);
    if (binding == null || server == null) {
      return;
    }
    final String text = binding.editor.getText().toString();
    final int line = binding.editor.getCursor().getLeftLine();
    final int column = binding.editor.getCursor().getLeftColumn();
    final String uri = uriOf(path);
    new Thread(
            () -> {
              List<Location> found = server.definition(uri, text, line, column);
              handler.post(() -> showDefinition(found));
            },
            "java-definition")
        .start();
  }

  private void showDefinition(@Nullable List<Location> found) {
    if (binding == null) {
      return;
    }
    if (found == null) {
      say(R.string.editor_java_not_available);
      return;
    }
    if (found.isEmpty()) {
      say(R.string.editor_java_nothing_found);
      return;
    }
    Location target = found.get(0);
    String prefix = JavaLanguageServer.uriOf(projectDirectory) + "/";
    if (!target.uri().startsWith(prefix)) {
      say(R.string.editor_java_outside_project);
      return;
    }
    open(Uri.decode(target.uri().substring(prefix.length())), target.line() + 1);
  }

  /** Shows what the server says about the symbol at the caret. */
  private void showHover() {
    final String path = currentPath;
    final JavaLanguageServer server = serverFor(path);
    if (binding == null || server == null) {
      return;
    }
    final String text = binding.editor.getText().toString();
    final int line = binding.editor.getCursor().getLeftLine();
    final int column = binding.editor.getCursor().getLeftColumn();
    final String uri = uriOf(path);
    new Thread(
            () -> {
              String info = server.hover(uri, text, line, column);
              handler.post(
                  () -> {
                    if (binding == null) {
                      return;
                    }
                    if (info == null) {
                      say(R.string.editor_java_not_available);
                    } else if (info.isEmpty()) {
                      say(R.string.editor_java_nothing_found);
                    } else {
                      new MaterialAlertDialogBuilder(requireContext())
                          .setMessage(info)
                          .setPositiveButton(android.R.string.ok, null)
                          .show();
                    }
                  });
            },
            "java-hover")
        .start();
  }

  /** Formats the shown Java file the way the server does. */
  private void formatDocument() {
    final String path = currentPath;
    final JavaLanguageServer server = serverFor(path);
    if (binding == null || server == null) {
      return;
    }
    final String text = binding.editor.getText().toString();
    final String uri = uriOf(path);
    new Thread(
            () -> {
              List<TextEdit> edits = server.format(uri, text, 4);
              handler.post(() -> applyFormatting(path, text, edits));
            },
            "java-format")
        .start();
  }

  private void applyFormatting(String path, String before, @Nullable List<TextEdit> edits) {
    if (binding == null) {
      return;
    }
    if (edits == null) {
      say(R.string.editor_java_not_available);
      return;
    }
    if (edits.isEmpty()) {
      say(R.string.editor_java_already_formatted);
      return;
    }
    // The edits are for the text the server saw: if the person typed since, they do not fit.
    if (!path.equals(currentPath) || !before.equals(binding.editor.getText().toString())) {
      say(R.string.editor_java_text_changed);
      return;
    }
    io.github.rosemoe.sora.text.Content content = binding.editor.getText();
    content.beginBatchEdit();
    for (TextEdit edit : edits) {
      int lastLine = content.getLineCount() - 1;
      int startLine = Math.min(edit.startLine(), lastLine);
      int endLine = Math.min(edit.endLine(), lastLine);
      content.replace(
          startLine,
          Math.min(edit.startCharacter(), content.getColumnCount(startLine)),
          endLine,
          Math.min(edit.endCharacter(), content.getColumnCount(endLine)),
          edit.newText());
    }
    content.endBatchEdit();
  }

  /** Hands the live text of the shown document to the view model, which saves it if changed. */
  private void snapshotCurrent() {
    handler.removeCallbacks(autoSave);
    if (binding != null && currentPath != null) {
      viewModel.snapshot(currentPath, binding.editor.getText().toString());
    }
  }

  private void closeTab(String path) {
    snapshotCurrent();
    viewModel.close(path);
  }

  private void render(EditorState state) {
    renderTabs(state);

    boolean hasTabs = !state.tabs().paths().isEmpty();
    binding.editor.setVisibility(hasTabs ? View.VISIBLE : View.GONE);
    binding.empty.setVisibility(hasTabs ? View.GONE : View.VISIBLE);

    // The language comes first, so the text is analysed with it. A rename can change it without
    // changing the text.
    SourceLanguage language = SourceLanguage.forPath(state.shownPath());
    if (language != appliedLanguage) {
      appliedLanguage = language;
      binding.editor.setEditorLanguage(languageFor(language));
    }
    if (state.revision() != appliedRevision) {
      appliedRevision = state.revision();
      binding.editor.setText(state.shownText());
      diagnostics = Collections.emptyList();
      binding.editor.invalidateDiagnostics();
      JavaLanguageServer shownServer = serverFor(state.shownPath());
      if (shownServer != null) {
        shownServer.start();
        shownServer.documentChanged(uriOf(state.shownPath()), state.shownText().toString());
      }
    }
    // A rename changes the shown path without changing the text, so follow it here.
    currentPath = state.shownPath();
    String shown = state.shownPath();
    boolean loading = shown != null && state.isLoading(shown);
    binding.editor.setEditable(shown != null && !loading);
    binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    hideSelectionMenu();
    updateStatus();
    updateUndoRedo();
    if (!loading) {
      applyPendingLine();
    }

    if (state.error() != null) {
      Snackbar.make(binding.getRoot(), EditorErrors.reason(state.error()), Snackbar.LENGTH_LONG)
          .show();
      viewModel.errorHandled();
    }
  }

  /**
   * The Sora language for a source language; null makes the editor show plain text. Java gets its
   * completion from the language server when that is ready (ADR-0031).
   */
  @Nullable
  private Language languageFor(SourceLanguage language) {
    Language base = TextMateSetup.languageFor(language);
    if (base == null || (language != SourceLanguage.JAVA && language != SourceLanguage.XML)) {
      return base;
    }
    return new ServerLanguage(
        base,
        (text, line, column) -> {
          String path = currentPath;
          JavaLanguageServer server = serverFor(path);
          if (path == null || server == null) {
            return null;
          }
          return server.complete(uriOf(path), text, line, column);
        });
  }

  private void renderTabs(EditorState state) {
    List<String> paths = state.tabs().paths();
    String active = state.tabs().activePath();
    if (paths.equals(renderedPaths) && state.dirty().equals(renderedDirty)) {
      selectTab(paths.indexOf(active));
      return;
    }
    for (String old : renderedPaths) {
      JavaLanguageServer closing = serverFor(old);
      if (closing != null && !paths.contains(old)) {
        closing.documentClosed(uriOf(old));
      }
    }
    renderedPaths = paths;
    renderedDirty = state.dirty();

    rendering = true;
    binding.tabs.removeAllTabs();
    for (String path : paths) {
      TabLayout.Tab tab = binding.tabs.newTab();
      tab.setTag(path);
      tab.setCustomView(tabView(path, state.isDirty(path)));
      binding.tabs.addTab(tab, path.equals(active));
    }
    rendering = false;
  }

  private void selectTab(int index) {
    if (index < 0) {
      return;
    }
    TabLayout.Tab tab = binding.tabs.getTabAt(index);
    if (tab != null && !tab.isSelected()) {
      rendering = true;
      tab.select();
      rendering = false;
    }
  }

  private View tabView(String path, boolean dirty) {
    ItemEditorTabBinding item = ItemEditorTabBinding.inflate(getLayoutInflater());
    String name = path.substring(path.lastIndexOf('/') + 1);
    item.title.setText(dirty ? getString(R.string.editor_tab_unsaved, name) : name);
    item.close.setOnClickListener(v -> closeTab(path));
    return item.getRoot();
  }
}
