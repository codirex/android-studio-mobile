package org.codirex.asm.domain.editor;

import java.util.Objects;

/** Reading or writing a document failed. {@link #reason()} says why, so callers need not parse text. */
public final class DocumentException extends Exception {

  /** Why an operation failed. */
  public enum Reason {
    /** The project or the file does not exist, or the path is a folder. */
    NOT_FOUND,
    /** The file belongs to the IDE itself, for example the project metadata. */
    NOT_ALLOWED,
    /** The file is not text: it contains NUL characters. */
    NOT_TEXT,
    /** The file is larger than {@link ProjectDocuments#MAX_BYTES}. */
    TOO_LARGE,
    /** The storage failed, or the path leaves the project. */
    STORAGE
  }

  private static final long serialVersionUID = 1L;

  private final Reason reason;

  public DocumentException(Reason reason, String message) {
    this(reason, message, null);
  }

  public DocumentException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = Objects.requireNonNull(reason, "reason");
  }

  public Reason reason() {
    return reason;
  }
}
