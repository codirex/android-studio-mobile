package org.codirex.asm.feature.environment;

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
import org.codirex.asm.domain.environment.EnvironmentVariable;
import org.codirex.asm.feature.environment.databinding.DialogVariableBinding;
import org.codirex.asm.feature.environment.databinding.FragmentEnvironmentBinding;

/**
 * The custom environment variables of the terminal: a list, a button to add one, a tap to change or
 * remove one. A change applies to terminal sessions started afterwards.
 */
@AndroidEntryPoint
public final class EnvironmentFragment extends Fragment {

  private EnvironmentViewModel viewModel;
  private FragmentEnvironmentBinding binding;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentEnvironmentBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(this).get(EnvironmentViewModel.class);
    VariableAdapter adapter = new VariableAdapter(this::edit);
    binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.list.setAdapter(adapter);
    binding.add.setOnClickListener(v -> edit(null));
    viewModel.state().observe(getViewLifecycleOwner(), state -> render(adapter, state));
  }

  @Override
  public void onStart() {
    super.onStart();
    requireActivity().setTitle(R.string.environment_title);
    viewModel.refresh();
  }

  @Override
  public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }

  private void render(VariableAdapter adapter, EnvironmentState state) {
    adapter.submitList(state.variables());
    binding.progress.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
    binding.empty.setVisibility(state.isEmpty() ? View.VISIBLE : View.GONE);
    EnvironmentFailure failure = state.failure();
    if (failure != null) {
      Snackbar.make(
              binding.getRoot(),
              getString(failure.text(), failure.detail()),
              Snackbar.LENGTH_LONG)
          .show();
      viewModel.failureHandled();
    }
  }

  /** The dialog to add a variable ({@code existing} is null) or to change or remove one. */
  private void edit(@Nullable final EnvironmentVariable existing) {
    final DialogVariableBinding dialogBinding =
        DialogVariableBinding.inflate(LayoutInflater.from(requireContext()));
    if (existing != null) {
      dialogBinding.nameInput.setText(existing.name());
      dialogBinding.nameInput.setEnabled(false);
      dialogBinding.valueInput.setText(existing.value());
      dialogBinding.valueInput.requestFocus();
    }
    MaterialAlertDialogBuilder builder =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(existing == null ? R.string.environment_add : R.string.environment_edit)
            .setView(dialogBinding.getRoot())
            .setPositiveButton(
                R.string.environment_save,
                (dialog, which) -> {
                  String name = String.valueOf(dialogBinding.nameInput.getText()).trim();
                  String value = String.valueOf(dialogBinding.valueInput.getText());
                  if (!name.isEmpty()) {
                    viewModel.save(name, value);
                  }
                })
            .setNegativeButton(android.R.string.cancel, null);
    if (existing != null) {
      builder.setNeutralButton(
          R.string.environment_remove, (dialog, which) -> viewModel.remove(existing.name()));
    }
    builder.show();
  }
}
