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
import java.io.File;
import org.codirex.asm.feature.toolchain.databinding.FragmentToolchainBinding;

/** The toolchain screen: the JDKs the IDE offers, whether each is installed, and how to install. */
@AndroidEntryPoint
public final class ToolchainFragment extends Fragment {

  /** The package of the userland that holds aapt2 (it is a subpackage of aapt). */
  private static final String AAPT2_PACKAGE = "aapt2";

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
    binding.aapt2Install.setOnClickListener(v -> navigator().installPackage(AAPT2_PACKAGE));
    viewModel.state().observe(getViewLifecycleOwner(), state -> render(adapter, state));
  }

  @Override
  public void onStart() {
    super.onStart();
    requireActivity().setTitle(R.string.toolchain_title);
    // Also covers coming back from the terminal that installed something: always fresh when shown.
    viewModel.refresh();
    showAapt2();
  }

  @Override
  public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }

  /** aapt2 is a program of the userland's bin folder: it is there or it is not. */
  private void showAapt2() {
    File aapt2 = new File(requireContext().getFilesDir(), "usr/bin/aapt2");
    boolean installed = aapt2.exists();
    if (installed) {
      binding.aapt2Details.setText(getString(R.string.aapt2_installed, aapt2.getAbsolutePath()));
    } else {
      binding.aapt2Details.setText(R.string.aapt2_hint);
    }
    binding.aapt2Install.setVisibility(installed ? View.GONE : View.VISIBLE);
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
