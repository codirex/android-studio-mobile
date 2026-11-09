package org.codirex.asm.feature.filetree;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.function.Consumer;
import org.codirex.asm.domain.filetree.FileTree;
import org.codirex.asm.feature.filetree.databinding.ItemTreeRowBinding;

/** The visible rows of the file tree, indented by depth. */
final class FileTreeAdapter extends ListAdapter<FileTree.Row, FileTreeAdapter.Holder> {

  private static final DiffUtil.ItemCallback<FileTree.Row> DIFF =
      new DiffUtil.ItemCallback<FileTree.Row>() {
        @Override
        public boolean areItemsTheSame(@NonNull FileTree.Row a, @NonNull FileTree.Row b) {
          return a.node().path().equals(b.node().path());
        }

        @Override
        public boolean areContentsTheSame(@NonNull FileTree.Row a, @NonNull FileTree.Row b) {
          return a.equals(b);
        }
      };

  private final Consumer<FileTree.Row> onClick;

  FileTreeAdapter(Consumer<FileTree.Row> onClick) {
    super(DIFF);
    this.onClick = onClick;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Holder(
        ItemTreeRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    holder.bind(getItem(position), onClick);
  }

  static final class Holder extends RecyclerView.ViewHolder {

    private final ItemTreeRowBinding binding;

    Holder(ItemTreeRowBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(FileTree.Row row, Consumer<FileTree.Row> onClick) {
      Context context = binding.getRoot().getContext();
      int indent = context.getResources().getDimensionPixelSize(R.dimen.tree_indent) * row.depth();
      ViewGroup.LayoutParams params = binding.indent.getLayoutParams();
      params.width = indent;
      binding.indent.setLayoutParams(params);

      boolean directory = row.node().isDirectory();
      binding.chevron.setVisibility(directory ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
      binding.chevron.setRotation(row.isExpanded() ? 90f : 0f);
      binding.icon.setImageResource(directory ? R.drawable.ic_tree_folder : R.drawable.ic_tree_file);
      binding.name.setText(row.node().name());
      binding.getRoot().setOnClickListener(v -> onClick.accept(row));
    }
  }
}
