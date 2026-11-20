package org.codirex.asm.domain.lsp;

/** A request failed: the server answered with an error, or the connection ended. */
public final class LspException extends Exception {

  /** The error codes of JSON-RPC that the client uses itself. */
  public static final int METHOD_NOT_FOUND = -32601;

  public static final int INTERNAL_ERROR = -32603;

  /** The code of a failure that is not an answer of the server (connection closed, ...). */
  public static final int CONNECTION = -1;

  private static final long serialVersionUID = 1L;

  private final int code;

  public LspException(int code, String message) {
    super(message);
    this.code = code;
  }

  public LspException(int code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public int code() {
    return code;
  }
}
