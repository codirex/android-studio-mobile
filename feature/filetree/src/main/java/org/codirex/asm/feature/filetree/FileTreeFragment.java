package org.codirex.asm.feature.filetree;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;
import org.codirex.asm.domain.filetree.FileTree;
import org.codirex.asm.domain.filetree.TreeNode;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.feature.filetree.databinding.FragmentFileTreeBinding;

/** The files of one project as a tree. Folders open and close on tap; a tapped file is reported. */
@AndroidEntryPoint
public final class FileTreeFragment extends Fragment {

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

    FileTreeAdapter adapter = new FileTreeAdapter(this::onRowClicked);
    binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.list.setAdapter(adapter);

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
  }

  private void onRowClicked(FileTree.Row row) {
    TreeNode node = row.node();
    if (node.isDirectory()) {
      viewModel.toggle(node);
    } else {
      ((FileTreeNavigator) requireActivity()).openFile(project, node.path());
    }
  }
}
