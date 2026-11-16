package org.codirex.asm.feature.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import org.codirex.asm.domain.template.TemplateFile;
import org.codirex.asm.feature.project.databinding.ItemTemplateBinding;

/** The rows of the files of a template in the editor. Positions are indexes into the file list. */
final class TemplateFileAdapter extends ListAdapter<TemplateFile, TemplateFileAdapter.Holder> {

  /** What happens to a row. */
  interface Listener {
    void onClick(int position);

    void onLongClick(int position);
  }

  private static final DiffUtil.ItemCallback<TemplateFile> DIFF =
      new DiffUtil.ItemCallback<TemplateFile>() {
        @Override
        public boolean areItemsTheSame(@NonNull TemplateFile a, @NonNull TemplateFile b) {
          return a.path().equals(b.path());
        }

        @Override
        public boolean areContentsTheSame(@NonNull TemplateFile a, @NonNull TemplateFile b) {
          return a.equals(b);
        }
      };

  private final Listener listener;

  TemplateFileAdapter(Listener listener) {
    super(DIFF);
    this.listener = listener;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Holder(
        ItemTemplateBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    holder.bind(getItem(position), listener);
  }

  static final class Holder extends RecyclerView.ViewHolder {

    private final ItemTemplateBinding binding;

    Holder(ItemTemplateBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(TemplateFile file, Listener listener) {
      Context context = binding.getRoot().getContext();
      binding.name.setText(file.path());
      binding.details.setText(
          context.getString(R.string.template_file_size, file.content().length()));
      binding.getRoot().setOnClickListener(v -> listener.onClick(getBindingAdapterPosition()));
      binding
          .getRoot()
          .setOnLongClickListener(
              v -> {
                listener.onLongClick(getBindingAdapterPosition());
                return true;
              });
    }
  }
}
