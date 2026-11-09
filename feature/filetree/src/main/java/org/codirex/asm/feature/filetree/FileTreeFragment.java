package org.codirex.asm.feature.filetree;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;
import org.codirex.asm.domain.filetree.EntryNames;
import org.codirex.asm.domain.filetree.FileTree;
import org.codirex.asm.domain.filetree.TreeNode;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.feature.filetree.databinding.DialogEntryNameBinding;
import org.codirex.asm.feature.filetree.databinding.FragmentFileTreeBinding;

/**
 * The files of one project as a tree. Folders open and close on tap; a tapped file is reported.
 * A long press offers new file, new folder and delete; two buttons create at the project root.
 */
@AndroidEntryPoint
public final class FileTreeFragment extends Fragment {

  private static final int MENU_NEW_FILE = 1;
  private static final int MENU_NEW_FOLDER = 2;
  private static final int MENU_DELETE = 3;

  private FileTreeViewModel viewModel;
  private FragmentFileTreeBinding binding;
  private ProjectName project;

  /** Creates the screen for {@code project}. */
  public static FileTreeFragment newInstance(ProjectName project) {
    Bundle arguments = new Bundle();
    arguments.putString(FileTreeViewModel.ARG_PROJECT, project.value());
    FileTreeFragment fragment = new FileTreeFragment();
    fragment.setArguments(arguments);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentFileTreeBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    project = ProjectName.of(requireArguments().getString(FileTreeViewModel.ARG_PROJECT));
    viewModel = new ViewModelProvider(this).get(FileTreeViewModel.class);

    FileTreeAdapter adapter = new FileTreeAdapter(this::onRowClicked, this::showMenu);
    binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.list.setAdapter(adapter);

    binding.newFile.setOnClickListener(v -> showNewEntryDialog(FileTree.ROOT, false));
    binding.newFolder.setOnClickListener(v -> showNewEntryDialog(FileTree.ROOT, true));

    viewModel.state().observe(getViewLifecycleOwner(), state -> render(adapter, state));
  }

  @Override
  public void onStart() {
    super.onStart();
    requireActivity().setTitle(project.value());
    // Also covers coming back from another screen: the tree is always fresh when shown.
    viewModel.refresh();
  }

  @Override
  public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }

  private void render(FileTreeAdapter adapter, FileTreeState state) {
    List<FileTree.Row> rows = state.tree().rows();
    adapter.submitList(rows);
    boolean loading = state.isLoading();
    binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    binding.empty.setVisibility(
        !loading && state.tree().isLoaded(FileTree.ROOT) && rows.isEmpty()
            ? View.VISIBLE
            : View.GONE);
    if (state.error() != null) {
      Snackbar.make(binding.getRoot(), TreeErrors.reason(state.error()), Snackbar.LENGTH_LONG)
          .show();
      viewModel.errorHandled();
    }
    TreeEvent event = state.event();
    if (event != null) {
      FileTreeNavigator navigator = (FileTreeNavigator) requireActivity();
      if (event.type() == TreeEvent.Type.OPEN) {
        navigator.openFile(project, event.path());
      } else {
        navigator.pathDeleted(project, event.path());
      }
      viewModel.eventHandled();
    }
  }

  private void onRowClicked(FileTree.Row row) {
    TreeNode node = row.node();
    if (node.isDirectory()) {
      viewModel.toggle(node);
    } else {
      ((FileTreeNavigator) requireActivity()).openFile(project, node.path());
    }
  }

  private void showMenu(View anchor, FileTree.Row row) {
    TreeNode node = row.node();
    PopupMenu menu = new PopupMenu(requireContext(), anchor);
    if (node.isDirectory()) {
      menu.getMenu().add(Menu.NONE, MENU_NEW_FILE, 0, R.string.tree_new_file);
      menu.getMenu().add(Menu.NONE, MENU_NEW_FOLDER, 1, R.string.tree_new_folder);
    }
    menu.getMenu().add(Menu.NONE, MENU_DELETE, 2, R.string.tree_delete);
    menu.setOnMenuItemClickListener(
        item -> {
          switch (item.getItemId()) {
            case MENU_NEW_FILE:
              showNewEntryDialog(node.path(), false);
              return true;
            case MENU_NEW_FOLDER:
              showNewEntryDialog(node.path(), true);
              return true;
            case MENU_DELETE:
              confirmDelete(node);
              return true;
            default:
              return false;
          }
        });
    menu.show();
  }

  private void showNewEntryDialog(String parentPath, boolean directory) {
    DialogEntryNameBinding input = DialogEntryNameBinding.inflate(getLayoutInflater());
    AlertDialog dialog =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(directory ? R.string.tree_new_folder : R.string.tree_new_file)
            .setView(input.getRoot())
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.tree_create, null)
            .create();
    // The button is wired after show() so an invalid name keeps the dialog open.
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      String name = String.valueOf(input.entryName.getText()).trim();
                      if (!EntryNames.isValid(name)) {
                        input.entryLayout.setError(getString(R.string.tree_error_invalid_name));
                        return;
                      }
                      dialog.dismiss();
                      viewModel.create(parentPath, name, directory);
                    }));
    dialog.show();
  }

  private void confirmDelete(TreeNode node) {
    int message =
        node.isDirectory()
            ? R.string.tree_delete_folder_message
            : R.string.tree_delete_file_message;
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.tree_delete)
        .setMessage(getString(message, node.name()))
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(R.string.tree_delete, (d, which) -> viewModel.delete(node))
        .show();
  }
}
