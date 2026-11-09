package org.codirex.asm.feature.filetree;

import org.codirex.asm.domain.filetree.FileTree;
import org.codirex.asm.domain.project.ProjectException.Reason;

/**
 * Everything the file tree screen shows. Immutable.
 *
 * <p>{@link #error()} is a one-time message: the screen shows it and then tells the view model it
 * was handled, so a rotation does not show it twice.
 */
final class FileTreeState {

  private final FileTree tree;
  private final int pending;
  private final Reason error;

  private FileTreeState(FileTree tree, int pending, Reason error) {
    this.tree = tree;
    this.pending = pending;
    this.error = error;
  }

  static FileTreeState initial() {
    return new FileTreeState(FileTree.empty(), 0, null);
  }

  /** A folder started loading. */
  FileTreeState started() {
    return new FileTreeState(tree, pending + 1, error);
  }

  /** The user opened or closed a folder; nothing is loading because of it yet. */
  FileTreeState withTree(FileTree newTree) {
    return new FileTreeState(newTree, pending, error);
  }

  /** A folder finished loading, or its job was cancelled. */
  FileTreeState finished(FileTree newTree) {
    return new FileTreeState(newTree, Math.max(0, pending - 1), error);
  }

  /** A folder could not be loaded. */
  FileTreeState failed(FileTree newTree, Reason reason) {
    return new FileTreeState(newTree, Math.max(0, pending - 1), reason);
  }

  FileTreeState errorHandled() {
    return new FileTreeState(tree, pending, null);
  }

  FileTree tree() {
    return tree;
  }

  boolean isLoading() {
    return pending > 0;
  }

  /** A failure not yet shown to the user, or null. */
  Reason error() {
    return error;
  }
}
