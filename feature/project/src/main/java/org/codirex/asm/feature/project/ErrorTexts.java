package org.codirex.asm.feature.project;

import androidx.annotation.StringRes;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.ProjectException.Reason;
import org.codirex.asm.domain.project.ProjectName;

/** Maps the domain's stable error codes to the texts the user reads. */
final class ErrorTexts {

  private ErrorTexts() {}

  @StringRes
  static int nameError(String code) {
    switch (code) {
      case ProjectName.ERROR_EMPTY:
        return R.string.error_name_empty;
      case ProjectName.ERROR_TOO_LONG:
        return R.string.error_name_too_long;
      case ProjectName.ERROR_RESERVED:
        return R.string.error_name_reserved;
      case ProjectName.ERROR_CHARACTER:
        return R.string.error_name_character;
      default:
        return R.string.error_invalid;
    }
  }

  @StringRes
  static int packageError(String code) {
    switch (code) {
      case PackageName.ERROR_EMPTY:
        return R.string.error_package_empty;
      case PackageName.ERROR_TOO_FEW_SEGMENTS:
        return R.string.error_package_too_few_segments;
      case PackageName.ERROR_SEGMENT_START:
        return R.string.error_package_segment_start;
      case PackageName.ERROR_CHARACTER:
        return R.string.error_package_character;
      case PackageName.ERROR_KEYWORD:
        return R.string.error_package_keyword;
      default:
        return R.string.error_invalid;
    }
  }

  @StringRes
  static int reason(Reason reason) {
    switch (reason) {
      case ALREADY_EXISTS:
        return R.string.error_name_exists;
      case NOT_FOUND:
        return R.string.error_not_found;
      case INVALID_METADATA:
        return R.string.error_invalid_metadata;
      case STORAGE:
      default:
        return R.string.error_storage;
    }
  }
}
