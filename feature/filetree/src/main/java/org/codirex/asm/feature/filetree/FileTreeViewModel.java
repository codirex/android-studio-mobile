package org.codirex.asm.feature.filetree;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.codirex.asm.core.concurrency.RealmFactory;
import org.codirex.asm.core.concurrency.RealmViewModel;
import org.codirex.asm.core.concurrency.UiStateHolder;
import org.codirex.asm.domain.filetree.FileTree;
import org.codirex.asm.domain.filetree.ProjectFiles;
import org.codirex.asm.domain.filetree.TreeNode;
import org.codirex.asm.domain.project.ProjectName;

/**
 * Loads the folders of one project off the main thread and keeps the {@link FileTree}. All methods
 * are called on the main thread. The project name comes from the fragment arguments.
 */
@HiltViewModel
final class FileTreeViewModel extends RealmViewModel {

  /** The fragment argument that carries the project name. */
  static final String ARG_PROJECT = "project";

  private final ProjectFiles files;
  private final ProjectName project;
  private final UiStateHolder<FileTreeState> state =
      new UiStateHolder<>(FileTreeState.initial());

  @Inject
  FileTreeViewModel(RealmFactory factory, ProjectFiles files, SavedStateHandle arguments) {
    super(factory);
    this.files = files;
    String name = arguments.get(ARG_PROJECT);
    this.project = ProjectName.of(name);
  }

  LiveData<FileTreeState> state() {
    return state.observable();
  }

  /** Reloads the root and every folder that was loaded before, replacing what they held. */
  void refresh() {
    Set<String> paths = new LinkedHashSet<>();
    paths.add(FileTree.ROOT);
    paths.addAll(state.get().tree().loadedPaths());
    for (String path : paths) {
      load(path);
    }
  }

  /** Opens or closes a folder; an opened folder that was never loaded is loaded now. */
  void toggle(TreeNode node) {
    if (!node.isDirectory()) {
      return;
    }
    FileTree tree = state.get().tree().toggled(node.path());
    state.set(state.get().withTree(tree));
    if (tree.isExpanded(node.path()) && !tree.isLoaded(node.path())) {
      load(node.path());
    }
  }

  /** The screen showed the error; do not show it again. */
  void errorHandled() {
    state.set(state.get().errorHandled());
  }

  private void load(String path) {
    state.set(state.get().started());
    this.<List<TreeNode>>runAsync(
        checkpoint -> files.list(project, path),
        outcome -> {
          FileTreeState current = state.get();
          if (outcome.isSuccess()) {
            state.set(current.finished(current.tree().withChildren(path, outcome.valueOrNull())));
          } else if (outcome.isFailure()) {
            // A folder that cannot be read is closed again so the screen does not show it empty.
            FileTree tree =
                FileTree.ROOT.equals(path) ? current.tree() : current.tree().withCollapsed(path);
            state.set(current.failed(tree, Failures.reasonOf(outcome.failureOrNull())));
          } else {
            state.set(current.finished(current.tree()));
          }
        });
  }
}
