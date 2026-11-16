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
import java.util.ArrayList;
import java.util.List;
import org.codirex.asm.domain.template.Template;
import org.codirex.asm.feature.project.databinding.DialogTemplateNameBinding;
import org.codirex.asm.feature.project.databinding.FragmentTemplateListBinding;

/**
 * The template manager: every template the IDE offers. A tap opens the actions of the template:
 * copy it into a new custom template (any template), change it or delete it (custom ones).
 */
@AndroidEntryPoint
public final class TemplateListFragment extends Fragment {

  private TemplateListViewModel viewModel;
  private FragmentTemplateListBinding binding;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentTemplateListBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(this).get(TemplateListViewModel.class);
    TemplateAdapter adapter = new TemplateAdapter(this::showActions);
    binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.list.setAdapter(adapter);
    viewModel.state().observe(getViewLifecycleOwner(), state -> render(adapter, state));
  }

  @Override
  public void onStart() {
    super.onStart();
    requireActivity().setTitle(R.string.template_list_title);
    // Also covers coming back from the editor: the list is always fresh when shown.
    viewModel.refresh();
  }

  @Override
  public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }

  private void render(TemplateAdapter adapter, TemplateListState state) {
    adapter.submitList(state.rows());
    binding.progress.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
    TemplateFailure failure = state.failure();
    if (failure != null) {
      Snackbar.make(
              binding.getRoot(),
              getString(failure.text(), failure.detail()),
              Snackbar.LENGTH_LONG)
          .show();
      viewModel.failureHandled();
    }
  }

  /** What can be done with the tapped template. */
  private void showActions(TemplateListState.Row row) {
    final Template template = row.template();
    List<CharSequence> labels = new ArrayList<>();
    if (row.isCustom()) {
      labels.add(getString(R.string.template_edit));
    }
    labels.add(getString(R.string.template_duplicate));
    if (row.isCustom()) {
      labels.add(getString(R.string.template_delete));
    }
    final boolean custom = row.isCustom();
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(template.name())
        .setItems(
            labels.toArray(new CharSequence[0]),
            (dialog, which) -> {
              // Custom templates list edit, duplicate, delete; built-in ones only duplicate.
              int action = custom ? which : 1;
              if (action == 0) {
                navigator().showTemplateEditor(template.id());
              } else if (action == 1) {
                askName(template);
              } else {
                confirmDelete(template);
              }
            })
        .show();
  }

  private void askName(Template source) {
    final DialogTemplateNameBinding dialogBinding =
        DialogTemplateNameBinding.inflate(LayoutInflater.from(requireContext()));
    dialogBinding.nameInput.setText(getString(R.string.template_copy_name, source.name()));
    dialogBinding.nameInput.selectAll();
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.template_duplicate_title)
        .setView(dialogBinding.getRoot())
        .setPositiveButton(
            R.string.template_duplicate,
            (dialog, which) -> {
              String name = String.valueOf(dialogBinding.nameInput.getText()).trim();
              if (!name.isEmpty()) {
                viewModel.duplicate(source, name);
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void confirmDelete(Template template) {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.template_delete_title)
        .setMessage(getString(R.string.template_delete_message, template.name()))
        .setPositiveButton(R.string.template_delete, (dialog, which) -> viewModel.delete(template))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private ProjectNavigator navigator() {
    return (ProjectNavigator) requireActivity();
  }
}
