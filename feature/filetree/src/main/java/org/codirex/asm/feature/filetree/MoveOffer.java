package org.codirex.asm.feature.filetree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codirex.asm.domain.filetree.TreeNode;

/**
 * The folders an entry can be moved to, ready to be shown. Immutable. {@link #none()} means there is
 * nothing to show.
 */
final class MoveOffer {

  private static final MoveOffer NONE = new MoveOffer(null, new ArrayList<String>());

  private final TreeNode node;
  private final List<String> targets;

  private MoveOffer(TreeNode node, List<String> targets) {
    this.node = node;
    this.targets = Collections.unmodifiableList(targets);
  }

  static MoveOffer none() {
    return NONE;
  }

  static MoveOffer of(TreeNode node, List<String> targets) {
    return new MoveOffer(node, new ArrayList<>(targets));
  }

  /** The entry to move, or {@code null} when there is no offer. */
  TreeNode node() {
    return node;
  }

  /** Folder paths; the empty path is the project root. May be empty: nowhere to move to. */
  List<String> targets() {
    return targets;
  }
}
