package org.codirex.asm.feature.sdk;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.function.Consumer;
import org.codirex.asm.domain.sdk.SdkEntry;
import org.codirex.asm.feature.sdk.databinding.ItemSdkBinding;

/** The rows of the package list. */
final class SdkAdapter extends ListAdapter<SdkEntry, SdkAdapter.Holder> {

  private static final DiffUtil.ItemCallback<SdkEntry> DIFF =
      new DiffUtil.ItemCallback<SdkEntry>() {
        @Override
        public boolean areItemsTheSame(@NonNull SdkEntry a, @NonNull SdkEntry b) {
          return a.pkg().path().equals(b.pkg().path());
        }

        @Override
        public boolean areContentsTheSame(@NonNull SdkEntry a, @NonNull SdkEntry b) {
          return a.pkg().revision().equals(b.pkg().revision())
              && a.isInstalled() == b.isInstalled()
              && a.isUpdatable() == b.isUpdatable()
              && a.isLicenseAccepted() == b.isLicenseAccepted();
        }
      };

  private final Consumer<SdkEntry> onClick;

  SdkAdapter(Consumer<SdkEntry> onClick) {
    super(DIFF);
    this.onClick = onClick;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Holder(
        ItemSdkBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    holder.bind(getItem(position), onClick);
  }

  static final class Holder extends RecyclerView.ViewHolder {

    private final ItemSdkBinding binding;

    Holder(ItemSdkBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(SdkEntry entry, Consumer<SdkEntry> onClick) {
      Context context = binding.getRoot().getContext();
      binding.name.setText(entry.pkg().displayName());
      long size = entry.pkg().archive().size();
      String sizeText = size > 0 ? Formatter.formatShortFileSize(context, size) : "";
      binding.detail.setText(
          context.getString(R.string.sdk_item_detail, entry.pkg().revision(), sizeText));
      int status;
      if (entry.isUpdatable()) {
        status = R.string.sdk_status_update;
      } else if (entry.isInstalled()) {
        status = R.string.sdk_status_installed;
      } else {
        status = R.string.sdk_status_available;
      }
      binding.status.setText(status);
      binding.getRoot().setOnClickListener(v -> onClick.accept(entry));
    }
  }
}
