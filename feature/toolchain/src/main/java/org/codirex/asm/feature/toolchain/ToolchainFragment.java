package org.codirex.asm.feature.toolchain;

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
import org.codirex.asm.feature.toolchain.databinding.FragmentToolchainBinding;

/** The toolchain screen: the JDKs the IDE offers, whether each is installed, and how to install. */
@AndroidEntryPoint
public final class ToolchainFragment extends Fragment {

  private ToolchainViewModel viewModel;
  private FragmentToolchainBinding binding;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentToolchainBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(this).get(ToolchainViewModel.class);
    JdkAdapter adapter =
        new JdkAdapter(status -> navigator().installPackage(status.offer().packageName()));
    binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.list.setAdapter(adapter);
    viewModel.state().observe(getViewLifecycleOwner(), state -> render(adapter, state));
  }

  @Override
  public void onStart() {
    super.onStart();
    requireActivity().setTitle(R.string.toolchain_title);
    // Also covers coming back from the terminal that installed something: always fresh when shown.
    viewModel.refresh();
  }

  @Override
  public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }

  private void render(JdkAdapter adapter, ToolchainState state) {
    adapter.submitList(state.jdks());
    binding.progress.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
    if (state.hasFailed()) {
      Snackbar.make(binding.getRoot(), R.string.toolchain_failed, Snackbar.LENGTH_LONG).show();
      viewModel.failureHandled();
    }
  }

  private ToolchainNavigator navigator() {
    return (ToolchainNavigator) requireActivity();
  }
}
