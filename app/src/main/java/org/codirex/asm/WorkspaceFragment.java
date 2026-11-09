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

  public static WorkspaceFragment newInstance(ProjectName project) {
    Bundle arguments = new Bundle();
    arguments.putString(ARG_PROJECT, project.value());
    WorkspaceFragment fragment = new WorkspaceFragment();
    fragment.setArguments(arguments);
    return fragment;
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
      ProjectName project = ProjectName.of(requireArguments().getString(ARG_PROJECT));
      getChildFragmentManager()
          .beginTransaction()
          .replace(R.id.tree_container, FileTreeFragment.newInstance(project))
          .replace(R.id.editor_container, EditorFragment.newInstance(project))
          .commit();
    }

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
    binding = null;
    super.onDestroyView();
  }

  /** Opens a file of the project in the editor and closes the drawer. */
  void openFile(String relativePath) {
    binding.drawer.closeDrawer(GravityCompat.START);
    EditorFragment editor = editor();
    if (editor != null) {
      editor.open(relativePath);
    }
  }

  /** A file or folder was deleted: the editor lets go of it. */
  void pathDeleted(String relativePath) {
    EditorFragment editor = editor();
    if (editor != null) {
      editor.closeUnder(relativePath);
    }
  }

  /** Shows the file tree. */
  void showFiles() {
    binding.drawer.openDrawer(GravityCompat.START);
  }

  private EditorFragment editor() {
    Fragment editor = getChildFragmentManager().findFragmentById(R.id.editor_container);
    return editor instanceof EditorFragment ? (EditorFragment) editor : null;
  }
}
