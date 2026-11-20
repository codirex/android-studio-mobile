package org.codirex.asm.domain.lsp;

/**
 * What the client hands to the person of the connection: messages the server starts. Called on the
 * thread that reads; keep it short and hand real work to another thread.
 */
public interface LspListener {

  /** A notification from the server, for example {@code textDocument/publishDiagnostics}. */
  void onNotification(String method, Object params);

  /**
   * A request from the server (for example {@code window/workDoneProgress/create}). The return
   * value is the result sent back; throw {@link LspException} to answer with an error.
   */
  Object onRequest(String method, Object params) throws LspException;

  /** The connection ended: the stream closed, or reading failed. Called once. */
  void onClosed(Throwable cause);
}
