package org.codirex.asm.feature.build;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import org.codirex.asm.domain.toolchain.BuildProblem;

/** The compiler's errors and warnings; touching one with a place opens the file. */
final class ProblemAdapter extends RecyclerView.Adapter<ProblemAdapter.Holder> {

  interface Listener {
    void onProblem(BuildProblem problem);
  }

  private final Listener listener;
  private List<BuildProblem> problems = new ArrayList<>();

  ProblemAdapter(Listener listener) {
    this.listener = listener;
  }

  void submit(List<BuildProblem> list) {
    if (list.size() == problems.size()) {
      return;
    }
    int old = problems.size();
    problems = new ArrayList<>(list);
    if (old == 0 || list.size() < old) {
      notifyDataSetChanged();
    } else {
      notifyItemRangeInserted(old, list.size() - old);
    }
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Holder(
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_problem, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    BuildProblem problem = problems.get(position);
    boolean error = problem.severity() == BuildProblem.Severity.ERROR;
    holder.message.setText(problem.message());
    holder.message.setTextColor(
        holder.message.getContext().getColor(error ? R.color.build_error : R.color.build_warning));
    if (problem.hasLocation()) {
      String name = problem.file().substring(problem.file().lastIndexOf('/') + 1);
      holder.place.setText(name + ":" + problem.line());
      holder.itemView.setOnClickListener(v -> listener.onProblem(problem));
      holder.itemView.setClickable(true);
    } else {
      holder.place.setText("");
      holder.itemView.setOnClickListener(null);
      holder.itemView.setClickable(false);
    }
  }

  @Override
  public int getItemCount() {
    return problems.size();
  }

  static final class Holder extends RecyclerView.ViewHolder {
    final TextView message;
    final TextView place;

    Holder(View view) {
      super(view);
      message = view.findViewById(R.id.problem_message);
      place = view.findViewById(R.id.problem_place);
    }
  }
}
