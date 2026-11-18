package org.codirex.asm.feature.environment;

import androidx.annotation.StringRes;
import org.codirex.asm.domain.environment.EnvironmentException;

/** Why an operation on the variables failed, in a form the screen can show. Immutable. */
final class EnvironmentFailure {

  private final EnvironmentException.Reason reason;
  private final String detail;

  private EnvironmentFailure(EnvironmentException.Reason reason, String detail) {
    this.reason = reason;
    this.detail = detail == null ? "" : detail;
  }

  /** Turns whatever a background job failed with into a failure the screen understands. */
  static EnvironmentFailure of(Throwable failure) {
    if (failure instanceof EnvironmentException) {
      EnvironmentException e = (EnvironmentException) failure;
      return new EnvironmentFailure(e.reason(), e.getMessage());
    }
    return new EnvironmentFailure(EnvironmentException.Reason.STORAGE, null);
  }

  /** The name the failure is about, for texts with a placeholder; may be empty. */
  String detail() {
    return detail;
  }

  /** The text of the failure; formats with {@link #detail()}. */
  @StringRes
  int text() {
    switch (reason) {
      case INVALID_NAME:
        return R.string.environment_error_invalid_name;
      case RESERVED_NAME:
        return R.string.environment_error_reserved_name;
      case INVALID_VALUE:
        return R.string.environment_error_invalid_value;
      case STORAGE:
      default:
        return R.string.environment_error_storage;
    }
  }
}
