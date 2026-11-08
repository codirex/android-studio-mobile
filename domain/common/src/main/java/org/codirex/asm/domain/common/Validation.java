package org.codirex.asm.domain.common;

import java.util.Objects;

/**
 * The outcome of checking a value: valid, or invalid with a stable error code.
 *
 * <p>The domain has no user-facing text. The code is a key such as {@code "name.empty"} that the UI
 * maps to a localized message.
 */
public final class Validation {

  private static final Validation OK = new Validation(null);

  private final String error;

  private Validation(String error) {
    this.error = error;
  }

  /** The value is valid. */
  public static Validation ok() {
    return OK;
  }

  /**
   * The value is invalid.
   *
   * @param code a stable, non-empty error code
   */
  public static Validation error(String code) {
    Objects.requireNonNull(code, "code");
    if (code.isEmpty()) {
      throw new IllegalArgumentException("code must not be empty");
    }
    return new Validation(code);
  }

  public boolean isValid() {
    return error == null;
  }

  /** The error code, or {@code null} if the value is valid. */
  public String error() {
    return error;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Validation && Objects.equals(error, ((Validation) other).error);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(error);
  }

  @Override
  public String toString() {
    return isValid() ? "Validation(ok)" : "Validation(" + error + ")";
  }
}
