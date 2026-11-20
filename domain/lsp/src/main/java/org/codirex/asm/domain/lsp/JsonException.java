package org.codirex.asm.domain.lsp;

/** A text is not valid JSON, or a value cannot be written as JSON. */
public final class JsonException extends Exception {

  private static final long serialVersionUID = 1L;

  public JsonException(String message) {
    super(message);
  }
}
