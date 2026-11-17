package org.codirex.asm.feature.toolchain;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.function.Consumer;
import org.codirex.asm.domain.toolchain.JdkStatus;
import org.codirex.asm.feature.toolchain.databinding.ItemJdkBinding;

/** The rows of the JDK list. */
final class JdkAdapter extends ListAdapter<JdkStatus, JdkAdapter.Holder> {

  private static final DiffUtil.ItemCallback<JdkStatus> DIFF =
      new DiffUtil.ItemCallback<JdkStatus>() {
        @Override
        public boolean areItemsTheSame(@NonNull JdkStatus a, @NonNull JdkStatus b) {
          return a.offer().equals(b.offer());
        }

        @Override
        public boolean areContentsTheSame(@NonNull JdkStatus a, @NonNull JdkStatus b) {
          if (a.isInstalled() != b.isInstalled()) {
            return false;
          }
          return !a.isInstalled() || a.installed().equals(b.installed());
        }
      };

  private final Consumer<JdkStatus> onInstall;

  JdkAdapter(Consumer<JdkStatus> onInstall) {
    super(DIFF);
    this.onInstall = onInstall;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Holder(ItemJdkBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    holder.bind(getItem(position), onInstall);
  }

  static final class Holder extends RecyclerView.ViewHolder {

    private final ItemJdkBinding binding;

    Holder(ItemJdkBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(JdkStatus status, Consumer<JdkStatus> onInstall) {
      Context context = binding.getRoot().getContext();
      binding.name.setText(context.getString(R.string.jdk_name, status.offer().majorVersion()));
      if (status.isInstalled()) {
        String version = status.installed().version();
        binding.details.setText(
            version.isEmpty()
                ? context.getString(R.string.jdk_installed, status.installed().home())
                : context.getString(
                    R.string.jdk_installed_version, version, status.installed().home()));
        binding.install.setVisibility(View.GONE);
      } else {
        binding.details.setText(R.string.jdk_not_installed);
        binding.install.setVisibility(View.VISIBLE);
        binding.install.setOnClickListener(v -> onInstall.accept(status));
      }
    }
  }
}
