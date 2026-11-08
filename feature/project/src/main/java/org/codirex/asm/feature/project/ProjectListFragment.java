package org.codirex.asm.feature.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import org.codirex.asm.domain.project.Project;
import org.codirex.asm.feature.project.databinding.FragmentProjectListBinding;

/** The list of projects, with a button to create one and a long press to delete one. */
@AndroidEntryPoint
public final class ProjectListFragment extends Fragment {

  private ProjectListViewModel viewModel;
  private FragmentProjectListBinding binding;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentProjectListBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(this).get(ProjectListViewModel.class);

    ProjectAdapter adapter =
        new ProjectAdapter(
            project -> navigator().showProject(project.name()), this::confirmDelete);
    binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.list.setAdapter(adapter);
    binding.newProject.setOnClickListener(v -> navigator().showCreateProject());

    viewModel.state().observe(getViewLifecycleOwner(), state -> render(adapter, state));
  }

  @Override
  public void onStart() {
    super.onStart();
    requireActivity().setTitle(R.string.project_list_title);
    // Also covers coming back from the create screen: the list is always fresh when shown.
    viewModel.refresh();
  }

  @Override
  public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }

  private void render(ProjectAdapter adapter, ProjectListState state) {
    adapter.submitList(state.projects());
    binding.progress.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
    binding.empty.setVisibility(
        !state.isLoading() && state.projects().isEmpty() ? View.VISIBLE : View.GONE);
    if (state.error() != null) {
      Snackbar.make(binding.getRoot(), ErrorTexts.reason(state.error()), Snackbar.LENGTH_LONG).show();
      viewModel.errorHandled();
    }
  }

  private void confirmDelete(Project project) {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.project_delete_title)
        .setMessage(getString(R.string.project_delete_message, project.name().value()))
        .setPositiveButton(R.string.project_delete_confirm, (dialog, which) -> viewModel.delete(project))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private ProjectNavigator navigator() {
    return (ProjectNavigator) requireActivity();
  }
}
