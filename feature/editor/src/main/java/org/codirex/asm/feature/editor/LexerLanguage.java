package org.codirex.asm.feature.editor;

import androidx.annotation.NonNull;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import org.codirex.asm.domain.editor.highlight.Lexer;

/** A language that has only highlighting, which comes from a {@link Lexer} (ADR-0026). */
final class LexerLanguage extends EmptyLanguage {

  private final LexerAnalyzeManager analyzer;

  LexerLanguage(Lexer lexer) {
    this.analyzer = new LexerAnalyzeManager(lexer);
  }

  @NonNull
  @Override
  public AnalyzeManager getAnalyzeManager() {
    return analyzer;
  }

  @Override
  public void destroy() {
    analyzer.destroy();
  }
}
