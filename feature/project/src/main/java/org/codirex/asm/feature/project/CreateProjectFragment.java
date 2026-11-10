package org.codirex.asm.feature.project;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;
import org.codirex.asm.domain.common.Validation;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.ProjectException.Reason;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.feature.project.databinding.FragmentCreateProjectBinding;

/** The form that creates a project. Fields are checked while the user types. */
@AndroidEntryPoint
public final class CreateProjectFragment extends Fragment {

  private static final String KEY_TYPE = "type";
  private static final String KEY_TEMPLATE = "template";

  private CreateProjectViewModel viewModel;
  private FragmentCreateProjectBinding binding;
  private final ProjectType[] types = ProjectType.values();
  private int selectedType;
  private int selectedTemplate;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentCreateProjectBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(this).get(CreateProjectViewModel.class);
    if (savedInstanceState != null) {
      selectedType = savedInstanceState.getInt(KEY_TYPE, 0);
      selectedTemplate = savedInstanceState.getInt(KEY_TEMPLATE, 0);
    }

    String[] labels = new String[types.length];
    for (int i = 0; i < types.length; i++) {
      labels[i] = getString(ProjectTypes.label(types[i]));
    }
    binding.typeInput.setAdapter(
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, labels));
    binding.typeInput.setText(labels[selectedType], false);
    binding.typeInput.setOnItemClickListener(
        (parent, v, position, id) -> {
          if (position != selectedType) {
            selectedType = position;
            selectedTemplate = 0;
            viewModel.showTemplates(types[selectedType]);
          }
        });
    binding.templateInput.setOnItemClickListener(
        (parent, v, position, id) -> selectedTemplate = position);

    binding.nameInput.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(Editable s) {
            showName(ProjectName.validate(s.toString()), s.length() == 0);
            updateButton();
          }
        });
    binding.packageInput.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(Editable s) {
            showPackage(PackageName.validate(s.toString()), s.length() == 0);
            updateButton();
          }
        });
    binding.create.setOnClickListener(
        v ->
            viewModel.create(
                text(binding.nameInput.getText()),
                text(binding.packageInput.getText()),
                types[selectedType],
                chosenTemplateId()));

    updateButton();
    viewModel.templates().observe(getViewLifecycleOwner(), this::renderTemplates);
    viewModel.state().observe(getViewLifecycleOwner(), this::render);
    viewModel.showTemplates(types[selectedType]);
  }

  @Override
  public void onStart() {
    super.onStart();
    requireActivity().setTitle(R.string.project_create_title);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(KEY_TYPE, selectedType);
    outState.putInt(KEY_TEMPLATE, selectedTemplate);
  }

  @Override
  public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }

  /** The id of the template shown, or {@code null} when no choice is offered (default template). */
  @Nullable
  private String chosenTemplateId() {
    TemplateChoices choices = viewModel.templates().getValue();
    if (choices == null
        || choices.type() != types[selectedType]
        || choices.items().size() < 2
        || selectedTemplate >= choices.items().size()) {
      return null;
    }
    return choices.items().get(selectedTemplate).id();
  }

  /** The template choice is shown only when the type has more than one template. */
  private void renderTemplates(TemplateChoices choices) {
    List<TemplateChoices.Item> items = choices.items();
    binding.templateLayout.setVisibility(items.size() > 1 ? View.VISIBLE : View.GONE);
    if (items.size() < 2) {
      return;
    }
    if (selectedTemplate >= items.size()) {
      selectedTemplate = 0;
    }
    String[] names = new String[items.size()];
    for (int i = 0; i < names.length; i++) {
      names[i] = items.get(i).name();
    }
    binding.templateInput.setAdapter(
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, names));
    binding.templateInput.setText(names[selectedTemplate], false);
  }

  private void render(CreateProjectState state) {
    boolean enabled = !state.busy();
    binding.progress.setVisibility(state.busy() ? View.VISIBLE : View.GONE);
    binding.nameLayout.setEnabled(enabled);
    binding.packageLayout.setEnabled(enabled);
    binding.typeLayout.setEnabled(enabled);
    binding.templateLayout.setEnabled(enabled);
    updateButton();

    if (state.isCreated()) {
      viewModel.eventHandled();
      ((ProjectNavigator) requireActivity()).onProjectCreated();
    } else if (state.failure() != null) {
      Reason reason = state.failure();
      viewModel.eventHandled();
      if (reason == Reason.ALREADY_EXISTS) {
        binding.nameLayout.setError(getString(ErrorTexts.reason(reason)));
      } else {
        Snackbar.make(binding.getRoot(), ErrorTexts.reason(reason), Snackbar.LENGTH_LONG).show();
      }
    }
  }

  /** An empty field shows no error while the user has not typed yet. */
  private void showName(Validation validation, boolean empty) {
    binding.nameLayout.setError(
        validation.isValid() || empty ? null : getString(ErrorTexts.nameError(validation.error())));
  }

  private void showPackage(Validation validation, boolean empty) {
    binding.packageLayout.setError(
        validation.isValid() || empty
            ? null
            : getString(ErrorTexts.packageError(validation.error())));
  }

  private void updateButton() {
    CreateProjectState state = viewModel.state().getValue();
    boolean busy = state != null && state.busy();
    boolean valid =
        ProjectName.validate(text(binding.nameInput.getText())).isValid()
            && PackageName.validate(text(binding.packageInput.getText())).isValid();
    binding.create.setEnabled(valid && !busy);
  }

  private static String text(Editable editable) {
    return editable == null ? "" : editable.toString();
  }
}
