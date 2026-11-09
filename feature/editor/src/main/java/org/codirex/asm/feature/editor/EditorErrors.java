package org.codirex.asm.feature.editor;

import androidx.annotation.StringRes;
import org.codirex.asm.domain.editor.DocumentException.Reason;

/** Maps the domain's failure reasons to the texts the user reads. */
final class EditorErrors {

  private EditorErrors() {}

  @StringRes
  static int reason(Reason reason) {
    switch (reason) {
      case NOT_FOUND:
        return R.string.editor_error_not_found;
      case NOT_ALLOWED:
        return R.string.editor_error_not_allowed;
      case NOT_TEXT:
        return R.string.editor_error_not_text;
      case TOO_LARGE:
        return R.string.editor_error_too_large;
      case STORAGE:
      default:
        return R.string.editor_error_storage;
    }
  }
}
