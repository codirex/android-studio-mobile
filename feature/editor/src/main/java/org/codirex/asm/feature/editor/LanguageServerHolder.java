package org.codirex.asm.feature.editor;

import androidx.lifecycle.ViewModel;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps the language servers of the open project (one per language) across a change of screen
 * configuration, so that turning the phone does not restart them. They are stopped when the editor
 * goes away for good.
 */
public final class LanguageServerHolder extends ViewModel {

  private final Map<String, JavaLanguageServer> servers = new HashMap<>();

  /** The server of a language, or null if none was made yet. */
  JavaLanguageServer server(String language) {
    return servers.get(language);
  }

  void setServer(String language, JavaLanguageServer server) {
    servers.put(language, server);
  }

  @Override
  protected void onCleared() {
    for (JavaLanguageServer server : servers.values()) {
      server.stop();
    }
    servers.clear();
  }
}
