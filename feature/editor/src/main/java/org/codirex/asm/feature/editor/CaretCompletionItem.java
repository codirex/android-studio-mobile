package org.codirex.asm.feature.editor;

import androidx.annotation.NonNull;
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.widget.CodeEditor;
import java.util.Collections;
import java.util.List;
import org.codirex.asm.domain.lsp.TextEdit;

/**
 * A proposal that puts the caret between the quotes when it inserts an attribute with an empty
 * value ({@code name=""}), so the value can be typed at once. It also knows its kind (for the badge
 * of the popup, {@link CompletionStyle}) and the letters typed so far (to show them in the label).
 */
final class CaretCompletionItem extends SimpleCompletionItem {

  private final int kind;
  private final String typed;
  private final List<TextEdit> extraEdits;
  private final int caretOffset;

  CaretCompletionItem(
      CharSequence label,
      CharSequence desc,
      int prefixLength,
      String commitText,
      int kind,
      String typed) {
    this(label, desc, prefixLength, commitText, kind, typed, null, -1);
  }

  /**
   * @param extraEdits changes elsewhere in the document (an import) that come with the proposal, in
   *     the places before it is taken, last position first
   * @param caretOffset where the caret goes inside {@code commitText}; -1 for its end
   */
  CaretCompletionItem(
      CharSequence label,
      CharSequence desc,
      int prefixLength,
      String commitText,
      int kind,
      String typed,
      List<TextEdit> extraEdits,
      int caretOffset) {
    super(label, desc, prefixLength, commitText);
    this.kind = kind;
    this.typed = typed == null ? "" : typed;
    this.extraEdits = extraEdits == null ? Collections.<TextEdit>emptyList() : extraEdits;
    this.caretOffset = caretOffset;
  }

  /** The kind of the protocol (2 method, 5 field, 7 class...); 0 if unknown. */
  int kind() {
    return kind;
  }

  /** The letters before the caret that this proposal completes. */
  String typed() {
    return typed;
  }

  @Override
  public void performCompletion(
      @NonNull CodeEditor editor, @NonNull Content text, int line, int column) {
    if (!extraEdits.isEmpty() || caretOffset >= 0) {
      applyWithExtras(editor, text, line, column);
      return;
    }
    super.performCompletion(editor, text, line, column);
    if (commitText != null && commitText.endsWith("=\"\"") && commitText.indexOf('\n') < 0) {
      editor.setSelection(line, column - prefixLength + commitText.length() - 1);
    }
  }

  /** The proposal and its extra edits, applied last position first so no place moves under them. */
  private void applyWithExtras(CodeEditor editor, Content text, int line, int column) {
    String insert = commitText == null ? "" : commitText;
    int start = column - prefixLength;
    int linesAbove = 0;
    boolean mainDone = false;
    for (TextEdit e : extraEdits) {
      boolean below = e.startLine() > line || (e.startLine() == line && e.startCharacter() >= start);
      if (!below && !mainDone) {
        text.replace(line, start, line, column, insert);
        mainDone = true;
      }
      if (e.startLine() > text.getLineCount() - 1) {
        continue;
      }
      text.replace(e.startLine(), e.startCharacter(), e.endLine(), e.endCharacter(), e.newText());
      if (!below) {
        linesAbove += count(e.newText(), '\n') - (e.endLine() - e.startLine());
      }
    }
    if (!mainDone) {
      text.replace(line, start, line, column, insert);
    }
    int at = caretOffset >= 0 ? Math.min(caretOffset, insert.length()) : insert.length();
    int newlines = count(insert.substring(0, at), '\n');
    int caretLine = line + linesAbove + newlines;
    int caretColumn =
        newlines == 0 ? start + at : at - (insert.lastIndexOf('\n', at - 1) + 1);
    editor.setSelection(caretLine, caretColumn);
  }

  private static int count(String s, char c) {
    int n = 0;
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == c) {
        n++;
      }
    }
    return n;
  }
}
