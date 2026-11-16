package org.codirex.asm.feature.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import org.codirex.asm.domain.template.TemplateFile;
import org.codirex.asm.feature.project.databinding.DialogTemplateFileBinding;
import org.codirex.asm.feature.project.databinding.FragmentTemplateEditBinding;

/**
 * Changes a custom template: its name and its files (add, change path or content, remove). The
 * placeholders the files may use are the ones of ADR-0017; the domain refuses a template that
 * uses an unknown one when it is saved.
 */
@AndroidEntryPoint
public final class TemplateEditFragment extends Fragment {

  private static final String ARG_ID = "template_id";

  /** A screen that edits the custom template with {@code templateId}. */
  public static TemplateEditFragment newInstance(String templateId) {
    TemplateEditFragment fragment = new TemplateEditFragment();
    Bundle args = new Bundle();
    args.putString(ARG_ID, templateId);
    fragment.setArguments(args);
    return fragment;
  }

  private TemplateEditViewModel viewModel;
  private FragmentTemplateEditBinding binding;
  private TemplateFileAdapter adapter;

  /** Whether the name field already got the loaded name: later states must not overwrite typing. */
  private boolean nameShown;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentTemplateEditBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(this).get(TemplateEditViewModel.class);
    nameShown = false;

    adapter =
        new TemplateFileAdapter(
            new TemplateFileAdapter.Listener() {
              @Override
              public void onClick(int position) {
                if (position >= 0) {
                  editFile(position);
                }
              }

              @Override
              public void onLongClick(int position) {
                confirmRemove(position);
              }
            });
    binding.files.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.files.setAdapter(adapter);

    binding.nameInput.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(android.text.Editable s) {
            viewModel.setName(s.toString());
          }
        });
    binding.addFile.setOnClickListener(v -> editFile(-1));
    binding.save.setOnClickListener(v -> viewModel.save());

    viewModel.state().observe(getViewLifecycleOwner(), this::render);
    String id = requireArguments().getString(ARG_ID, "");
    viewModel.load(id);
  }

  @Override
  public void onStart() {
    super.onStart();
    requireActivity().setTitle(R.string.template_edit_title);
  }

  @Override
  public void onDestroyView() {
    binding = null;
    adapter = null;
    super.onDestroyView();
  }

  private void render(TemplateEditState state) {
    TemplateEditState.Phase phase = state.phase();
    boolean editing = phase == TemplateEditState.Phase.EDITING;
    boolean saving = phase == TemplateEditState.Phase.SAVING;
    binding.progress.setVisibility(
        phase == TemplateEditState.Phase.LOADING || saving ? View.VISIBLE : View.GONE);
    binding.content.setVisibility(editing || saving ? View.VISIBLE : View.GONE);
    binding.missing.setVisibility(phase == TemplateEditState.Phase.MISSING ? View.VISIBLE : View.GONE);
    binding.save.setEnabled(editing);
    binding.addFile.setEnabled(editing);

    if ((editing || saving) && !nameShown) {
      nameShown = true;
      binding.nameInput.setText(state.name());
    }
    adapter.submitList(state.files());

    TemplateFailure failure = state.failure();
    if (failure != null) {
      Snackbar.make(
              binding.getRoot(),
              getString(failure.text(), failure.detail()),
              Snackbar.LENGTH_LONG)
          .show();
      viewModel.failureHandled();
    }
    if (phase == TemplateEditState.Phase.SAVED) {
      navigator().onTemplateSaved();
    }
  }

  /** Asks for the path and the content of the file at {@code position}, or of a new file (-1). */
  private void editFile(final int position) {
    TemplateEditState state = viewModel.state().getValue();
    if (state == null || state.phase() != TemplateEditState.Phase.EDITING) {
      return;
    }
    List<TemplateFile> files = state.files();
    TemplateFile current = position >= 0 && position < files.size() ? files.get(position) : null;

    final DialogTemplateFileBinding dialogBinding =
        DialogTemplateFileBinding.inflate(LayoutInflater.from(requireContext()));
    if (current != null) {
      dialogBinding.pathInput.setText(current.path());
      dialogBinding.contentInput.setText(current.content());
    }
    final AlertDialog dialog =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.template_file_title)
            .setView(dialogBinding.getRoot())
            .setPositiveButton(R.string.template_file_ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create();
    dialog.show();
    // Set after show(), so a path that is not given keeps the dialog open.
    dialog
        .getButton(AlertDialog.BUTTON_POSITIVE)
        .setOnClickListener(
            v -> {
              String path = String.valueOf(dialogBinding.pathInput.getText()).trim();
              if (path.isEmpty()) {
                dialogBinding.pathLayout.setError(getString(R.string.template_error_path));
                return;
              }
              String content = String.valueOf(dialogBinding.contentInput.getText());
              viewModel.putFile(position, new TemplateFile(path, content));
              dialog.dismiss();
            });
  }

  private void confirmRemove(final int position) {
    TemplateEditState state = viewModel.state().getValue();
    if (state == null
        || state.phase() != TemplateEditState.Phase.EDITING
        || position < 0
        || position >= state.files().size()) {
      return;
    }
    String path = state.files().get(position).path();
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.template_file_remove_title)
        .setMessage(getString(R.string.template_file_remove_message, path))
        .setPositiveButton(
            R.string.template_file_remove, (dialog, which) -> viewModel.removeFile(position))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private ProjectNavigator navigator() {
    return (ProjectNavigator) requireActivity();
  }
}
