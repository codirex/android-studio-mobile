package org.codirex.asm.feature.environment;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.function.Consumer;
import org.codirex.asm.domain.environment.EnvironmentVariable;
import org.codirex.asm.feature.environment.databinding.ItemVariableBinding;

/** The rows of the variable list. */
final class VariableAdapter extends ListAdapter<EnvironmentVariable, VariableAdapter.Holder> {

  private static final DiffUtil.ItemCallback<EnvironmentVariable> DIFF =
      new DiffUtil.ItemCallback<EnvironmentVariable>() {
        @Override
        public boolean areItemsTheSame(
            @NonNull EnvironmentVariable a, @NonNull EnvironmentVariable b) {
          return a.name().equals(b.name());
        }

        @Override
        public boolean areContentsTheSame(
            @NonNull EnvironmentVariable a, @NonNull EnvironmentVariable b) {
          return a.equals(b);
        }
      };

  private final Consumer<EnvironmentVariable> onClick;

  VariableAdapter(Consumer<EnvironmentVariable> onClick) {
    super(DIFF);
    this.onClick = onClick;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Holder(
        ItemVariableBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    holder.bind(getItem(position), onClick);
  }

  static final class Holder extends RecyclerView.ViewHolder {

    private final ItemVariableBinding binding;

    Holder(ItemVariableBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(EnvironmentVariable variable, Consumer<EnvironmentVariable> onClick) {
      binding.name.setText(variable.name());
      binding.value.setText(variable.value());
      binding.getRoot().setOnClickListener(v -> onClick.accept(variable));
    }
  }
}
