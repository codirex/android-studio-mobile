package org.codirex.asm.feature.editor;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.QuickQuoteHandler;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.widget.SymbolPairMatch;
import java.util.List;
import org.codirex.asm.domain.lsp.CompletionItem;

/**
 * A language of the editor that keeps everything of another one (the TextMate highlighting,
 * indentation, brackets) and takes the completion from a language server when it has an answer
 * (ADR-0031). Without an answer the other language's own completion is used.
 */
final class ServerLanguage implements Language {

  /** Where the proposals come from. Called on a worker thread; may block for a short time. */
  interface Source {
    /** The proposals at a position of the shown document, or null if the server cannot say. */
    @Nullable
    List<CompletionItem> complete(String documentText, int line, int column);
  }

  private final Language base;
  private final Source source;

  ServerLanguage(Language base, Source source) {
    this.base = base;
    this.source = source;
  }

  @NonNull
  @Override
  public AnalyzeManager getAnalyzeManager() {
    return base.getAnalyzeManager();
  }

  @Override
  public int getInterruptionLevel() {
    return base.getInterruptionLevel();
  }

  @Override
  public void requireAutoComplete(
      @NonNull ContentReference content,
      @NonNull CharPosition position,
      @NonNull CompletionPublisher publisher,
      @NonNull Bundle extraArguments)
      throws CompletionCancelledException {
    List<CompletionItem> items = null;
    try {
      items = source.complete(content.getReference().toString(), position.line, position.column);
    } catch (RuntimeException e) {
      // A failing server must not break typing: fall back to the editor's own proposals.
    }
    if (items == null) {
      base.requireAutoComplete(content, position, publisher, extraArguments);
      return;
    }
    String prefix =
        CompletionHelper.computePrefix(content, position, Character::isJavaIdentifierPart);
    for (CompletionItem item : items) {
      int replace = prefix.length();
      if (item.hasRange() && item.startLine() == position.line && item.endLine() == position.line) {
        replace = position.column - item.startCharacter();
      }
      if (replace < 0 || replace > position.column) {
        replace = prefix.length();
      }
      publisher.addItem(
          new CaretCompletionItem(item.label(), item.detail(), replace, item.insertText()));
    }
    publisher.updateList(true);
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference content, int line, int column) {
    return base.getIndentAdvance(content, line, column);
  }

  @Override
  public int getIndentAdvance(
      @NonNull ContentReference content,
      int line,
      int column,
      int spaceCountOnLine,
      int tabCountOnLine) {
    return base.getIndentAdvance(content, line, column, spaceCountOnLine, tabCountOnLine);
  }

  @Override
  public boolean useTab() {
    return base.useTab();
  }

  @NonNull
  @Override
  public Formatter getFormatter() {
    return base.getFormatter();
  }

  @Override
  public SymbolPairMatch getSymbolPairs() {
    return base.getSymbolPairs();
  }

  @Nullable
  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return base.getNewlineHandlers();
  }

  @Nullable
  @Override
  public QuickQuoteHandler getQuickQuoteHandler() {
    return base.getQuickQuoteHandler();
  }

  @Override
  public void destroy() {
    base.destroy();
  }
}
