package org.codirex.asm.domain.lsp;

import java.util.List;

/** Receives the diagnostics of a document each time the server publishes them. */
public interface DiagnosticsListener {

  /**
   * Called on the thread that reads from the server. {@code diagnostics} replaces the earlier ones
   * of that document; an empty list clears them.
   */
  void onDiagnostics(String uri, List<Diagnostic> diagnostics);
}
