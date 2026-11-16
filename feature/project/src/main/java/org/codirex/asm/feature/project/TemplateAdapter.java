package org.codirex.asm.feature.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.function.Consumer;
import org.codirex.asm.feature.project.databinding.ItemTemplateBinding;

/** The rows of the template list. */
final class TemplateAdapter extends ListAdapter<TemplateListState.Row, TemplateAdapter.Holder> {

  private static final DiffUtil.ItemCallback<TemplateListState.Row> DIFF =
      new DiffUtil.ItemCallback<TemplateListState.Row>() {
        @Override
        public boolean areItemsTheSame(
            @NonNull TemplateListState.Row a, @NonNull TemplateListState.Row b) {
          return a.template().id().equals(b.template().id());
        }

        @Override
        public boolean areContentsTheSame(
            @NonNull TemplateListState.Row a, @NonNull TemplateListState.Row b) {
          // A saved template is a new object with the same id; show it again.
          return a.template() == b.template();
        }
      };

  private final Consumer<TemplateListState.Row> onClick;

  TemplateAdapter(Consumer<TemplateListState.Row> onClick) {
    super(DIFF);
    this.onClick = onClick;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Holder(
        ItemTemplateBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    holder.bind(getItem(position), onClick);
  }

  static final class Holder extends RecyclerView.ViewHolder {

    private final ItemTemplateBinding binding;

    Holder(ItemTemplateBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(TemplateListState.Row row, Consumer<TemplateListState.Row> onClick) {
      Context context = binding.getRoot().getContext();
      binding.name.setText(row.template().name());
      binding.details.setText(
          context.getString(
              R.string.template_details,
              context.getString(ProjectTypes.label(row.template().type())),
              context.getString(
                  row.isCustom() ? R.string.template_custom : R.string.template_builtin)));
      binding.getRoot().setOnClickListener(v -> onClick.accept(row));
    }
  }
}
