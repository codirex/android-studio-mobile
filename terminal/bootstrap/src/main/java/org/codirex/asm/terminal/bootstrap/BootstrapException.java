package org.codirex.asm.terminal.bootstrap;

import java.util.Objects;

/** The userland could not be installed. {@link #reason()} says why. */
public final class BootstrapException extends Exception {

  /** Why the installation failed. */
  public enum Reason {
    /** The archive is damaged, unsafe or not a bootstrap archive. */
    INVALID_ARCHIVE,
    /** Reading the archive or writing the files failed. */
    STORAGE,
    /** There is no archive for this architecture (yet). */
    UNAVAILABLE,
    /** The download failed: no connection, a timeout or an error answer from the server. */
    NETWORK,
    /** The downloaded archive is not the one the app expects; it was deleted. */
    CHECKSUM,
    /** The caller cancelled the download. */
    CANCELLED
  }

  private static final long serialVersionUID = 1L;

  private final Reason reason;

  public BootstrapException(Reason reason, String message) {
    this(reason, message, null);
  }

  public BootstrapException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = Objects.requireNonNull(reason, "reason");
  }

  public Reason reason() {
    return reason;
  }
}
