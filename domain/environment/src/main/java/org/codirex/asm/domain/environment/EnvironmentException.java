package org.codirex.asm.domain.environment;

import java.util.Objects;

/** A variable could not be accepted or stored. {@link #reason()} says why. */
public final class EnvironmentException extends Exception {

  /** Why an operation failed. */
  public enum Reason {
    /** The name is empty or not made of letters, digits and underscores (not starting with a digit). */
    INVALID_NAME,
    /** The name belongs to the IDE: the terminal needs it to work. */
    RESERVED_NAME,
    /** The value is too long or contains a NUL character. */
    INVALID_VALUE,
    /** The storage failed. */
    STORAGE
  }

  private static final long serialVersionUID = 1L;

  private final Reason reason;

  public EnvironmentException(Reason reason, String message) {
    this(reason, message, null);
  }

  public EnvironmentException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = Objects.requireNonNull(reason, "reason");
  }

  public Reason reason() {
    return reason;
  }
}
