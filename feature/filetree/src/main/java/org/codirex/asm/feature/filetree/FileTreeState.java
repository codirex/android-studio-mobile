package org.codirex.asm.feature.filetree;

import org.codirex.asm.domain.filetree.FileTree;
import org.codirex.asm.domain.project.ProjectException.Reason;

/**
 * Everything the file tree screen shows. Immutable.
 *
 * <p>{@link #error()} and {@link #event()} are one-time messages: the screen handles them and then
 * tells the view model, so a rotation does not handle them twice.
 */
final class FileTreeState {

  private final FileTree tree;
  private final int pending;
  private final Reason error;
  private final TreeEvent event;

  private FileTreeState(FileTree tree, int pending, Reason error, TreeEvent event) {
    this.tree = tree;
    this.pending = pending;
    this.error = error;
    this.event = event;
  }

  static FileTreeState initial() {
    return new FileTreeState(FileTree.empty(), 0, null, null);
  }

  /** A job started: a folder is loading, or an entry is being created or deleted. */
  FileTreeState started() {
    return new FileTreeState(tree, pending + 1, error, event);
  }

  /** The user opened or closed a folder; nothing is loading because of it yet. */
  FileTreeState withTree(FileTree newTree) {
    return new FileTreeState(newTree, pending, error, event);
  }

  /** A job finished, or was cancelled. */
  FileTreeState finished(FileTree newTree) {
    return new FileTreeState(newTree, Math.max(0, pending - 1), error, event);
  }

  /** A job failed. */
  FileTreeState failed(FileTree newTree, Reason reason) {
    return new FileTreeState(newTree, Math.max(0, pending - 1), reason, event);
  }

  /** The host must be told about {@code newEvent}. */
  FileTreeState withEvent(TreeEvent newEvent) {
    return new FileTreeState(tree, pending, error, newEvent);
  }

  FileTreeState errorHandled() {
    return new FileTreeState(tree, pending, null, event);
  }

  FileTreeState eventHandled() {
    return new FileTreeState(tree, pending, error, null);
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

  /** An event not yet passed to the host, or null. */
  TreeEvent event() {
    return event;
  }
}
