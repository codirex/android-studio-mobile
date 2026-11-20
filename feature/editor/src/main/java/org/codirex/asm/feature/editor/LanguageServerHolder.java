package org.codirex.asm.feature.editor;

import androidx.lifecycle.ViewModel;

/**
 * Keeps the language server of the open project across a change of screen configuration, so that
 * turning the phone does not restart it. It is stopped when the editor goes away for good.
 */
public final class LanguageServerHolder extends ViewModel {

  private JavaLanguageServer java;

  /** The Java server, or null if none was made yet. */
  JavaLanguageServer java() {
    return java;
  }

  void setJava(JavaLanguageServer server) {
    this.java = server;
  }

  @Override
  protected void onCleared() {
    if (java != null) {
      java.stop();
      java = null;
    }
  }
}
