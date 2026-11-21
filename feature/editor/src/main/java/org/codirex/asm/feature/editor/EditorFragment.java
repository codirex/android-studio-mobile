package org.codirex.asm.feature.editor;

import android.content.res.Configuration;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticProvider;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.codirex.asm.domain.editor.SourceLanguage;
import org.codirex.asm.domain.lsp.Diagnostic;
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

  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable autoSave = this::snapshotCurrent;

  private EditorViewModel viewModel;
  private FragmentEditorBinding binding;

  /** The document whose text the editor widget holds, or null. */
  private volatile String currentPath;

  /** The Java language server of the project (ADR-0031), or null before the view exists. */
  private JavaLanguageServer javaServer;

  private File projectDirectory;
  private boolean installOffered;

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
    if (holder.java() == null) {
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

    binding.showFiles.setOnClickListener(
        v -> ((EditorNavigator) requireActivity()).showFileTree());
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
    if (javaServer != null) {
      javaServer.setListener(null);
    }
    binding.editor.unregisterDiagnosticProvider(diagnosticProvider);
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
    if (isJava(currentPath)) {
      handler.removeCallbacks(syncServer);
      handler.postDelayed(syncServer, SERVER_SYNC_DELAY_MS);
    }
  }

  private static boolean isJava(@Nullable String path) {
    return path != null && SourceLanguage.forPath(path) == SourceLanguage.JAVA;
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
    if (binding != null && javaServer != null && isJava(path)) {
      javaServer.documentChanged(uriOf(path), binding.editor.getText().toString());
    }
  }

  private void onServerState(JavaLanguageServer.State state) {
    if (binding == null) {
      return;
    }
    if (state == JavaLanguageServer.State.MISSING && !installOffered) {
      installOffered = true;
      Snackbar.make(binding.getRoot(), R.string.editor_java_server_missing, Snackbar.LENGTH_INDEFINITE)
          .setAction(
              R.string.editor_java_server_install,
              v -> ((EditorNavigator) requireActivity()).installPackage("jdtls"))
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

  /** Gives the editor the squiggles for the diagnostics of the shown file. */
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
      regions.add(new DiagnosticRegion(start, end, severity));
    }
    container.addDiagnostics(regions);
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
      if (isJava(state.shownPath()) && javaServer != null) {
        javaServer.start();
        javaServer.documentChanged(uriOf(state.shownPath()), state.shownText().toString());
      }
    }
    // A rename changes the shown path without changing the text, so follow it here.
    currentPath = state.shownPath();
    String shown = state.shownPath();
    boolean loading = shown != null && state.isLoading(shown);
    binding.editor.setEditable(shown != null && !loading);
    binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);

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
    if (base == null || language != SourceLanguage.JAVA) {
      return base;
    }
    return new ServerLanguage(
        base,
        (text, line, column) -> {
          String path = currentPath;
          JavaLanguageServer server = javaServer;
          if (path == null || server == null || !isJava(path)) {
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
    if (javaServer != null) {
      for (String old : renderedPaths) {
        if (!paths.contains(old) && isJava(old)) {
          javaServer.documentClosed(uriOf(old));
        }
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
