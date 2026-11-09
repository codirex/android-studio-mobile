package org.codirex.asm.feature.filetree;

import androidx.annotation.StringRes;
import org.codirex.asm.domain.project.ProjectException.Reason;

/** Maps the domain's failure reasons to the texts the user reads. */
final class TreeErrors {

  private TreeErrors() {}

  @StringRes
  static int reason(Reason reason) {
    switch (reason) {
      case NOT_FOUND:
        return R.string.tree_error_not_found;
      case ALREADY_EXISTS:
        return R.string.tree_error_exists;
      case INVALID_METADATA:
      case STORAGE:
      default:
        return R.string.tree_error_storage;
    }
  }
}
