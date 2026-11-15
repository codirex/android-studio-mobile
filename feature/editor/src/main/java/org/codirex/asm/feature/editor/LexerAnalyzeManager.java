package org.codirex.asm.feature.editor;

import io.github.rosemoe.sora.lang.analysis.SimpleAnalyzeManager;
import io.github.rosemoe.sora.lang.styling.MappedSpans;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import java.util.List;
import org.codirex.asm.domain.editor.highlight.Lexer;
import org.codirex.asm.domain.editor.highlight.Token;
import org.codirex.asm.domain.editor.highlight.TokenKind;

/**
 * Colours the text of the editor with the tokens of a {@link Lexer} (ADR-0026). The whole text is
 * analysed again after every change, on a background thread; the lexers are fast enough for the
 * files of a project, and a change that arrives meanwhile cancels the run that is outdated.
 */
final class LexerAnalyzeManager extends SimpleAnalyzeManager<Void> {

  private static final long NORMAL = TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL);

  private final Lexer lexer;

  LexerAnalyzeManager(Lexer lexer) {
    this.lexer = lexer;
  }

  @Override
  protected Styles analyze(StringBuilder text, Delegate<Void> delegate) {
    MappedSpans.Builder builder = new MappedSpans.Builder();
    List<Token> tokens = lexer.tokenize(text);
    if (delegate.isCancelled()) {
      return null;
    }
    Position position = new Position(text);
    int cursor = 0;
    for (Token token : tokens) {
      if (token.start() > cursor) {
        position.moveTo(cursor);
        builder.addIfNeeded(position.line, position.column(), NORMAL);
      }
      position.moveTo(token.start());
      builder.addIfNeeded(position.line, position.column(), TextStyle.makeStyle(colorOf(token.kind())));
      cursor = token.end();
    }
    if (cursor < text.length()) {
      position.moveTo(cursor);
      builder.addIfNeeded(position.line, position.column(), NORMAL);
    }
    builder.addNormalIfNull();
    position.moveTo(text.length());
    builder.determine(position.line);
    Styles styles = new Styles(builder.build());
    styles.finishBuilding();
    return styles;
  }

  private static int colorOf(TokenKind kind) {
    switch (kind) {
      case KEYWORD:
      case HEADING:
        return EditorColorScheme.KEYWORD;
      case COMMENT:
        return EditorColorScheme.COMMENT;
      case STRING:
      case NUMBER:
      case CODE:
        return EditorColorScheme.LITERAL;
      case TAG:
        return EditorColorScheme.HTML_TAG;
      case ATTRIBUTE_NAME:
        return EditorColorScheme.ATTRIBUTE_NAME;
      case ATTRIBUTE_VALUE:
      case LINK:
        return EditorColorScheme.ATTRIBUTE_VALUE;
      case OPERATOR:
        return EditorColorScheme.OPERATOR;
      case BUILTIN:
        return EditorColorScheme.FUNCTION_NAME;
      case EMPHASIS:
        return EditorColorScheme.ANNOTATION;
      default:
        return EditorColorScheme.TEXT_NORMAL;
    }
  }

  /** Walks forward through the text and knows the line and column of where it is. */
  private static final class Position {
    private final CharSequence text;
    private int offset;
    private int line;
    private int lineStart;

    Position(CharSequence text) {
      this.text = text;
    }

    void moveTo(int target) {
      while (offset < target) {
        char c = text.charAt(offset);
        offset++;
        boolean lineBreak = c == '\n' || (c == '\r' && !(offset < text.length() && text.charAt(offset) == '\n'));
        if (lineBreak) {
          line++;
          lineStart = offset;
        }
      }
    }

    int column() {
      return offset - lineStart;
    }
  }
}
