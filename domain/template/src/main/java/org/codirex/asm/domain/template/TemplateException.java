package org.codirex.asm.domain.template;

import java.util.Objects;

/** A template could not be rendered or written. {@link #reason()} says why. */
public final class TemplateException extends Exception {

  /** Why an operation failed. */
  public enum Reason {
    /** A placeholder names a variable that does not exist. */
    UNKNOWN_VARIABLE,
    /** A placeholder is not closed. */
    MALFORMED,
    /** A rendered path is absolute, leaves the project or points into the internal folder. */
    UNSAFE_PATH,
    /** The storage failed while writing the files. */
    STORAGE,
    /** A template does not meet the rules for saving it (id, name, variables, files, size). */
    INVALID
  }

  private static final long serialVersionUID = 1L;

  private final Reason reason;

  public TemplateException(Reason reason, String message) {
    this(reason, message, null);
  }

  public TemplateException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = Objects.requireNonNull(reason, "reason");
  }

  public Reason reason() {
    return reason;
  }
}
