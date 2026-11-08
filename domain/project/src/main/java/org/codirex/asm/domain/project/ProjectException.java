package org.codirex.asm.domain.project;

import java.util.Objects;

/** A project operation failed. {@link #reason()} says why, so callers can react without parsing text. */
public final class ProjectException extends Exception {

  /** Why an operation failed. */
  public enum Reason {
    /** A project with this name exists already. */
    ALREADY_EXISTS,
    /** No project has this name. */
    NOT_FOUND,
    /** The project's metadata is missing fields, unreadable or from a newer version. */
    INVALID_METADATA,
    /** The storage failed: disk full, permissions, I/O error. */
    STORAGE
  }

  private static final long serialVersionUID = 1L;

  private final Reason reason;

  public ProjectException(Reason reason, String message) {
    this(reason, message, null);
  }

  public ProjectException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = Objects.requireNonNull(reason, "reason");
  }

  public Reason reason() {
    return reason;
  }
}
