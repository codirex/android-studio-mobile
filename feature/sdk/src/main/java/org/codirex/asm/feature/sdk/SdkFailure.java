package org.codirex.asm.feature.sdk;

import androidx.annotation.StringRes;
import org.codirex.asm.domain.sdk.SdkException;

/** Why an SDK operation failed, in a form the screen can show. Immutable. */
final class SdkFailure {

  private final SdkException.Reason reason;

  private SdkFailure(SdkException.Reason reason) {
    this.reason = reason;
  }

  /** Turns whatever a background job failed with into a failure the screen understands. */
  static SdkFailure of(Throwable failure) {
    if (failure instanceof SdkException) {
      return new SdkFailure(((SdkException) failure).reason());
    }
    return new SdkFailure(null);
  }

  @StringRes
  int text() {
    if (reason == null) {
      return R.string.sdk_error_unknown;
    }
    switch (reason) {
      case NETWORK:
        return R.string.sdk_error_network;
      case CATALOG:
        return R.string.sdk_error_catalog;
      case NOT_OFFERED:
        return R.string.sdk_error_not_offered;
      case LICENSE_NOT_ACCEPTED:
        return R.string.sdk_error_license;
      case CHECKSUM:
        return R.string.sdk_error_checksum;
      case CANCELLED:
        return R.string.sdk_error_cancelled;
      case STORAGE:
      default:
        return R.string.sdk_error_storage;
    }
  }
}
