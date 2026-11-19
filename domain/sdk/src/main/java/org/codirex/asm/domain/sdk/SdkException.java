package org.codirex.asm.domain.sdk;

import java.util.Objects;

/** An SDK operation failed. {@link #reason()} says why. */
public final class SdkException extends Exception {

  /** Why an operation failed. */
  public enum Reason {
    /** The catalog or an archive could not be downloaded. */
    NETWORK,
    /** The catalog could not be read: it is not the format the IDE knows. */
    CATALOG,
    /** The package is not one the IDE offers. */
    NOT_OFFERED,
    /** The license of the package has not been accepted. */
    LICENSE_NOT_ACCEPTED,
    /** The downloaded archive does not match its checksum. */
    CHECKSUM,
    /** The person cancelled. */
    CANCELLED,
    /** The files could not be unpacked, moved or removed. */
    STORAGE
  }

  private static final long serialVersionUID = 1L;

  private final Reason reason;

  public SdkException(Reason reason, String message) {
    this(reason, message, null);
  }

  public SdkException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = Objects.requireNonNull(reason, "reason");
  }

  public Reason reason() {
    return reason;
  }
}
