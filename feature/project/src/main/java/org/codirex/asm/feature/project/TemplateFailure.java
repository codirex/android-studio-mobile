package org.codirex.asm.feature.project;

import androidx.annotation.StringRes;
import org.codirex.asm.domain.template.TemplateException;
import org.codirex.asm.domain.template.TemplateException.Reason;

/** Why a template operation failed, in a form the screens can show. Immutable. */
final class TemplateFailure {

  private final Reason reason;
  private final String detail;

  private TemplateFailure(Reason reason, String detail) {
    this.reason = reason;
    this.detail = detail == null ? "" : detail;
  }

  /** Turns whatever a background job failed with into a failure the screens understand. */
  static TemplateFailure of(Throwable failure) {
    if (failure instanceof TemplateException) {
      TemplateException e = (TemplateException) failure;
      return new TemplateFailure(e.reason(), e.getMessage());
    }
    return new TemplateFailure(Reason.STORAGE, null);
  }

  Reason reason() {
    return reason;
  }

  /** The domain's message: it names the broken rule or the offending path or variable. */
  String detail() {
    return detail;
  }

  /** The text of the failure; formats with {@link #detail()} when the text has a placeholder. */
  @StringRes
  int text() {
    switch (reason) {
      case UNKNOWN_VARIABLE:
        return R.string.template_error_unknown_variable;
      case MALFORMED:
        return R.string.template_error_malformed;
      case UNSAFE_PATH:
        return R.string.template_error_unsafe_path;
      case INVALID:
        return R.string.template_error_invalid;
      case STORAGE:
      default:
        return R.string.error_storage;
    }
  }
}
