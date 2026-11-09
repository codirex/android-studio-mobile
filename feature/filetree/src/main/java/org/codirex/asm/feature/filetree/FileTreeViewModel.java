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
 * Loads the folders of one project off the main thread, keeps the {@link FileTree} and creates,
 * renames and deletes entries. All methods are called on the main thread. The project name comes
 * from the fragment arguments.
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

  /**
   * Creates a file or folder named {@code name} (already validated) in {@code parentPath}. The
   * parent opens so the new entry is visible; a new file is also reported to be opened.
   */
  void create(String parentPath, String name, boolean directory) {
    String path = parentPath.isEmpty() ? name : parentPath + "/" + name;
    state.set(state.get().started());
    this.<Boolean>runAsync(
        checkpoint -> {
          files.create(project, parentPath, name, directory);
          return Boolean.TRUE;
        },
        outcome -> {
          FileTreeState current = state.get();
          if (outcome.isSuccess()) {
            FileTree tree = current.tree();
            if (!parentPath.isEmpty()) {
              tree = tree.withExpanded(parentPath);
            }
            FileTreeState next = current.finished(tree);
            state.set(directory ? next : next.withEvent(TreeEvent.open(path)));
            load(parentPath);
          } else if (outcome.isFailure()) {
            state.set(current.failed(current.tree(), Failures.reasonOf(outcome.failureOrNull())));
          } else {
            state.set(current.finished(current.tree()));
          }
        });
  }

  /**
   * Gives an entry a new name (already validated) in the same folder, then reloads that folder. A
   * renamed folder shows closed, because what was loaded under the old path is dropped.
   */
  void rename(TreeNode node, String newName) {
    String path = node.path();
    String parent = node.parentPath();
    String newPath = parent.isEmpty() ? newName : parent + "/" + newName;
    state.set(state.get().started());
    this.<Boolean>runAsync(
        checkpoint -> {
          files.rename(project, path, newName);
          return Boolean.TRUE;
        },
        outcome -> {
          FileTreeState current = state.get();
          if (outcome.isSuccess()) {
            FileTree tree = current.tree().without(path);
            state.set(current.finished(tree).withEvent(TreeEvent.renamed(path, newPath)));
            load(parent);
          } else if (outcome.isFailure()) {
            state.set(current.failed(current.tree(), Failures.reasonOf(outcome.failureOrNull())));
          } else {
            state.set(current.finished(current.tree()));
          }
        });
  }

  /** Deletes a file, or a folder with its content, then reloads the folder that held it. */
  void delete(TreeNode node) {
    String path = node.path();
    String parent = node.parentPath();
    state.set(state.get().started());
    this.<Boolean>runAsync(
        checkpoint -> {
          files.delete(project, path);
          return Boolean.TRUE;
        },
        outcome -> {
          FileTreeState current = state.get();
          if (outcome.isSuccess()) {
            FileTree tree = current.tree().without(path);
            state.set(current.finished(tree).withEvent(TreeEvent.deleted(path)));
            load(parent);
          } else if (outcome.isFailure()) {
            state.set(current.failed(current.tree(), Failures.reasonOf(outcome.failureOrNull())));
          } else {
            state.set(current.finished(current.tree()));
          }
        });
  }

  /** The screen showed the error; do not show it again. */
  void errorHandled() {
    state.set(state.get().errorHandled());
  }

  /** The screen passed the event to the host; do not pass it again. */
  void eventHandled() {
    state.set(state.get().eventHandled());
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
