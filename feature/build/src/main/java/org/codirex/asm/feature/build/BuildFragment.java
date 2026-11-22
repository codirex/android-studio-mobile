package org.codirex.asm.feature.build;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.List;
import org.codirex.asm.domain.toolchain.BuildReport;
import org.codirex.asm.feature.build.databinding.FragmentBuildBinding;

/** Runs a Gradle task of a project and shows what it says: tasks, errors, the failure, the result. */
public final class BuildFragment extends Fragment {

  private static final String ARG_DIRECTORY = "directory";
  private static final String ARG_TASK = "task";

  /**
   * @param projectDirectory the folder of the project (files/projects/&lt;name&gt;)
   * @param task the Gradle task, such as {@code assembleDebug}
   */
  public static BuildFragment newInstance(String projectDirectory, String task) {
    Bundle args = new Bundle();
    args.putString(ARG_DIRECTORY, projectDirectory);
    args.putString(ARG_TASK, task);
    BuildFragment fragment = new BuildFragment();
    fragment.setArguments(args);
    return fragment;
  }

  private BuildViewModel viewModel;
  private FragmentBuildBinding binding;
  private ProblemAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentBuildBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(this).get(BuildViewModel.class);
    String task = requireArguments().getString(ARG_TASK, "");
    requireActivity().setTitle(task);
    adapter =
        new ProblemAdapter(
            problem -> ((BuildNavigator) requireActivity()).openBuildFile(problem.file()));
    binding.problems.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.problems.setAdapter(adapter);
    binding.stop.setOnClickListener(v -> viewModel.stop());
    viewModel.state().observe(getViewLifecycleOwner(), this::render);
    viewModel.start(requireArguments().getString(ARG_DIRECTORY, ""), task);
  }

  @Override
  public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }

  private void render(BuildState state) {
    BuildReport report = state.report();
    boolean running = !state.isFinished();
    binding.progress.setVisibility(running ? View.VISIBLE : View.GONE);
    binding.stop.setVisibility(running ? View.VISIBLE : View.GONE);

    if (!state.error().isEmpty()) {
      binding.status.setText(getString(R.string.build_could_not_start, state.error()));
    } else if (report.status() == BuildReport.Status.SUCCESS) {
      binding.status.setText(getString(R.string.build_successful, report.duration()));
    } else if (report.status() == BuildReport.Status.FAILED) {
      binding.status.setText(getString(R.string.build_failed, report.duration()));
    } else if (state.isFinished()) {
      binding.status.setText(R.string.build_stopped);
    } else {
      List<BuildReport.Task> tasks = report.tasks();
      binding.status.setText(
          tasks.isEmpty()
              ? getString(R.string.build_starting)
              : getString(R.string.build_running, tasks.get(tasks.size() - 1).name()));
    }
    binding.summary.setText(
        getString(
            R.string.build_summary,
            report.tasks().size(),
            report.errorCount(),
            report.warningCount()));

    binding.failure.setVisibility(report.failure().isEmpty() ? View.GONE : View.VISIBLE);
    binding.failure.setText(report.failure());
    adapter.submit(report.problems());

    // What the parser does not understand (a missing program, a stack trace) is still worth seeing.
    boolean unexplained =
        state.isFinished()
            && report.status() != BuildReport.Status.SUCCESS
            && report.problems().isEmpty()
            && report.failure().isEmpty();
    binding.output.setVisibility(unexplained ? View.VISIBLE : View.GONE);
    binding.output.setText(TextUtils.join("\n", state.tail()));
  }
}
