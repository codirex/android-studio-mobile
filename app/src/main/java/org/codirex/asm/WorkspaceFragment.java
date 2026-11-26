package org.codirex.asm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import dagger.hilt.android.AndroidEntryPoint;
import org.codirex.asm.databinding.FragmentWorkspaceBinding;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.feature.editor.EditorFragment;
import org.codirex.asm.feature.filetree.FileTreeFragment;

/**
 * The screen of an open project: the file tree in a drawer and the editor next to it. It is
 * composed here, in the app, because features must not know each other (ADR-0010, ADR-0014).
 */
@AndroidEntryPoint
public final class WorkspaceFragment extends Fragment {

  private static final String ARG_PROJECT = "project";

  private FragmentWorkspaceBinding binding;
  private android.view.ViewTreeObserver.OnGlobalLayoutListener keyboardListener;

  public static WorkspaceFragment newInstance(ProjectName project) {
    Bundle arguments = new Bundle();
    arguments.putString(ARG_PROJECT, project.value());
    WorkspaceFragment fragment = new WorkspaceFragment();
    fragment.setArguments(arguments);
    return fragment;
  }

  /** The project this workspace shows. */
  ProjectName project() {
    return ProjectName.of(requireArguments().getString(ARG_PROJECT));
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentWorkspaceBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      ProjectName project = project();
      getChildFragmentManager()
          .beginTransaction()
          .replace(R.id.tree_container, FileTreeFragment.newInstance(project))
          .replace(R.id.editor_container, EditorFragment.newInstance(project))
          .commit();
    }

    binding.dockFiles.setOnClickListener(v -> showFiles());
    binding.dockBuild.setOnClickListener(
        v -> ((MainActivity) requireActivity()).showGradleTasks());
    binding.dockTerminal.setOnClickListener(v -> ((MainActivity) requireActivity()).showTerminal());
    // The dock gives its room back to the code while the keyboard is open.
    keyboardListener =
        () -> {
          if (binding == null) {
            return;
          }
          android.graphics.Rect frame = new android.graphics.Rect();
          binding.getRoot().getWindowVisibleDisplayFrame(frame);
          int covered = binding.getRoot().getRootView().getHeight() - frame.bottom;
          boolean keyboard = covered > Math.round(150 * getResources().getDisplayMetrics().density);
          int visibility = keyboard ? View.GONE : View.VISIBLE;
          binding.dock.setVisibility(visibility);
          binding.dockDivider.setVisibility(visibility);
        };
    binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);

    // The back button closes the drawer first, and only then leaves the project.
    final OnBackPressedCallback closeDrawer =
        new OnBackPressedCallback(false) {
          @Override
          public void handleOnBackPressed() {
            binding.drawer.closeDrawer(GravityCompat.START);
          }
        };
    requireActivity()
        .getOnBackPressedDispatcher()
        .addCallback(getViewLifecycleOwner(), closeDrawer);
    binding.drawer.addDrawerListener(
        new DrawerLayout.SimpleDrawerListener() {
          @Override
          public void onDrawerOpened(View drawerView) {
            closeDrawer.setEnabled(true);
          }

          @Override
          public void onDrawerClosed(View drawerView) {
            closeDrawer.setEnabled(false);
          }
        });
  }

  @Override
  public void onDestroyView() {
    if (keyboardListener != null) {
      android.view.ViewTreeObserver observer = binding.getRoot().getViewTreeObserver();
      if (observer.isAlive()) {
        observer.removeOnGlobalLayoutListener(keyboardListener);
      }
      keyboardListener = null;
    }
    binding = null;
    super.onDestroyView();
  }

  /** Opens a file of the project in the editor and closes the drawer. */
  void openFile(String relativePath) {
    openFile(relativePath, 0);
  }

  /** Opens a file and puts the cursor on {@code line} (1-based; 0 means the top). */
  void openFile(String relativePath, int line) {
    binding.drawer.closeDrawer(GravityCompat.START);
    EditorFragment editor = editor();
    if (editor != null) {
      editor.open(relativePath, line);
    }
  }

  /** A file or folder was deleted: the editor lets go of it. */
  void pathDeleted(String relativePath) {
    EditorFragment editor = editor();
    if (editor != null) {
      editor.closeUnder(relativePath);
    }
  }

  /** A file or folder was renamed: the editor follows it. */
  void pathRenamed(String oldPath, String newPath) {
    EditorFragment editor = editor();
    if (editor != null) {
      editor.renameUnder(oldPath, newPath);
    }
  }

  /** Shows the file tree. */
  void showFiles() {
    binding.drawer.openDrawer(GravityCompat.START);
  }

  /** Formats the shown file. */
  void format() {
    EditorFragment editor = editor();
    if (editor != null) {
      editor.runFormat();
    }
  }

  /** Goes to the definition of the symbol at the caret. */
  void definition() {
    EditorFragment editor = editor();
    if (editor != null) {
      editor.runDefinition();
    }
  }

  /** Shows information about the symbol at the caret. */
  void info() {
    EditorFragment editor = editor();
    if (editor != null) {
      editor.runInfo();
    }
  }

  private EditorFragment editor() {
    Fragment editor = getChildFragmentManager().findFragmentById(R.id.editor_container);
    return editor instanceof EditorFragment ? (EditorFragment) editor : null;
  }
}
