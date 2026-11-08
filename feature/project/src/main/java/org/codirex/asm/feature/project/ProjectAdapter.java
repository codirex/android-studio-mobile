package org.codirex.asm.feature.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.function.Consumer;
import org.codirex.asm.domain.project.Project;
import org.codirex.asm.feature.project.databinding.ItemProjectBinding;

/** The rows of the project list. */
final class ProjectAdapter extends ListAdapter<Project, ProjectAdapter.Holder> {

  private static final DiffUtil.ItemCallback<Project> DIFF =
      new DiffUtil.ItemCallback<Project>() {
        @Override
        public boolean areItemsTheSame(@NonNull Project a, @NonNull Project b) {
          return a.name().equals(b.name());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Project a, @NonNull Project b) {
          return a.equals(b);
        }
      };

  private final Consumer<Project> onClick;
  private final Consumer<Project> onLongClick;

  ProjectAdapter(Consumer<Project> onClick, Consumer<Project> onLongClick) {
    super(DIFF);
    this.onClick = onClick;
    this.onLongClick = onLongClick;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Holder(ItemProjectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    holder.bind(getItem(position), onClick, onLongClick);
  }

  static final class Holder extends RecyclerView.ViewHolder {

    private final ItemProjectBinding binding;

    Holder(ItemProjectBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(Project project, Consumer<Project> onClick, Consumer<Project> onLongClick) {
      Context context = binding.getRoot().getContext();
      binding.name.setText(project.name().value());
      binding.details.setText(
          context.getString(
              R.string.project_details,
              context.getString(ProjectTypes.label(project.type())),
              project.packageName().value()));
      binding.getRoot().setOnClickListener(v -> onClick.accept(project));
      binding.getRoot()
          .setOnLongClickListener(
              v -> {
                onLongClick.accept(project);
                return true;
              });
    }
  }
}
