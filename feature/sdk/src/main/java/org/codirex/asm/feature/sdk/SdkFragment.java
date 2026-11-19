package org.codirex.asm.feature.sdk;

import android.os.Bundle;
import android.text.format.Formatter;
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
import org.codirex.asm.domain.sdk.SdkEntry;
import org.codirex.asm.domain.sdk.SdkPackage;
import org.codirex.asm.feature.sdk.databinding.FragmentSdkBinding;

/**
 * The Android SDK packages the IDE can install: platforms and build tools. A tap on a package asks
 * for the license (the first time), then installs; an installed package can be updated or removed.
 */
@AndroidEntryPoint
public final class SdkFragment extends Fragment {

  private SdkViewModel viewModel;
  private FragmentSdkBinding binding;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentSdkBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(this).get(SdkViewModel.class);
    SdkAdapter adapter = new SdkAdapter(this::choose);
    binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.list.setAdapter(adapter);
    binding.cancel.setOnClickListener(v -> viewModel.cancel());
    viewModel.state().observe(getViewLifecycleOwner(), state -> render(adapter, state));
    viewModel.progress().observe(getViewLifecycleOwner(), this::renderProgress);
  }

  @Override
  public void onStart() {
    super.onStart();
    requireActivity().setTitle(R.string.sdk_title);
    viewModel.refresh();
  }

  @Override
  public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }

  private void render(SdkAdapter adapter, SdkState state) {
    adapter.submitList(state.entries());
    binding.progress.setVisibility(
        state.isLoading() || state.isBusy() ? View.VISIBLE : View.GONE);
    binding.empty.setVisibility(state.isEmpty() ? View.VISIBLE : View.GONE);
    binding.busyRow.setVisibility(state.isBusy() ? View.VISIBLE : View.GONE);
    if (state.isBusy()) {
      binding.busyText.setText(getString(R.string.sdk_busy, state.busyName()));
    } else {
      binding.progress.setIndeterminate(true);
    }
    SdkFailure failure = state.failure();
    if (failure != null) {
      Snackbar.make(binding.getRoot(), failure.text(), Snackbar.LENGTH_LONG).show();
      viewModel.failureHandled();
    }
  }

  private void renderProgress(Integer percent) {
    if (binding == null || percent == null) {
      return;
    }
    if (percent < 0) {
      binding.progress.setIndeterminate(true);
    } else {
      binding.progress.setIndeterminate(false);
      binding.progress.setProgressCompat(percent, true);
    }
  }

  private void choose(SdkEntry entry) {
    if (viewModel.state().getValue() != null && viewModel.state().getValue().isBusy()) {
      return;
    }
    if (entry.isInstalled()) {
      offerChange(entry);
    } else {
      offerInstall(entry);
    }
  }

  /** The package is not installed: the license first if it was not accepted, else a confirmation. */
  private void offerInstall(SdkEntry entry) {
    final SdkPackage pkg = entry.pkg();
    MaterialAlertDialogBuilder builder =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(pkg.displayName())
            .setNegativeButton(android.R.string.cancel, null);
    if (entry.isLicenseAccepted()) {
      builder
          .setMessage(
              getString(
                  R.string.sdk_confirm_install,
                  Formatter.formatShortFileSize(requireContext(), Math.max(pkg.archive().size(), 0))))
          .setPositiveButton(R.string.sdk_install, (d, w) -> viewModel.install(pkg));
    } else {
      builder
          .setMessage(pkg.license().text().trim())
          .setPositiveButton(R.string.sdk_accept_install, (d, w) -> viewModel.install(pkg));
    }
    builder.show();
  }

  /** The package is installed: update it if the catalog has a newer revision, or remove it. */
  private void offerChange(SdkEntry entry) {
    final SdkPackage pkg = entry.pkg();
    MaterialAlertDialogBuilder builder =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(pkg.displayName())
            .setMessage(getString(R.string.sdk_installed_revision, entry.installedRevision()))
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.sdk_remove, (d, w) -> viewModel.remove(pkg));
    if (entry.isUpdatable()) {
      builder.setPositiveButton(
          R.string.sdk_update,
          (d, w) -> {
            if (entry.isLicenseAccepted()) {
              viewModel.install(pkg);
            } else {
              offerInstall(entry);
            }
          });
    }
    builder.show();
  }
}
