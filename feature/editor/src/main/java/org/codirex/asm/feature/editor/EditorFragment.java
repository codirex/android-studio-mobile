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
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable autoSave = this::snapshotCurrent;

  private EditorViewModel viewModel;
  private FragmentEditorBinding binding;

  /** The document whose text the editor widget holds, or null. */
  private String currentPath;

  private int appliedRevision = -1;
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

    // A new view starts empty, whatever the previous one showed.
    currentPath = null;
    appliedRevision = -1;
    renderedPaths = Collections.emptyList();
    renderedDirty = Collections.emptySet();

    boolean night =
        (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
    binding.editor.setColorScheme(night ? new SchemeDarcula() : new SchemeGitHub());
    binding.editor.subscribeAlways(ContentChangeEvent.class, this::onContentChanged);

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

    if (state.revision() != appliedRevision) {
      appliedRevision = state.revision();
      currentPath = state.shownPath();
      binding.editor.setText(state.shownText());
    }
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

  private void renderTabs(EditorState state) {
    List<String> paths = state.tabs().paths();
    String active = state.tabs().activePath();
    if (paths.equals(renderedPaths) && state.dirty().equals(renderedDirty)) {
      selectTab(paths.indexOf(active));
      return;
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
