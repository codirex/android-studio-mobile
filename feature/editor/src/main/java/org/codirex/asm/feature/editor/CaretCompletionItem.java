package org.codirex.asm.feature.editor;

import androidx.annotation.NonNull;
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * A proposal that puts the caret between the quotes when it inserts an attribute with an empty
 * value ({@code name=""}), so the value can be typed at once. It also knows its kind (for the badge
 * of the popup, {@link CompletionStyle}) and the letters typed so far (to show them in the label).
 */
final class CaretCompletionItem extends SimpleCompletionItem {

  private final int kind;
  private final String typed;

  CaretCompletionItem(
      CharSequence label,
      CharSequence desc,
      int prefixLength,
      String commitText,
      int kind,
      String typed) {
    super(label, desc, prefixLength, commitText);
    this.kind = kind;
    this.typed = typed == null ? "" : typed;
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
    super.performCompletion(editor, text, line, column);
    if (commitText != null && commitText.endsWith("=\"\"") && commitText.indexOf('\n') < 0) {
      editor.setSelection(line, column - prefixLength + commitText.length() - 1);
    }
  }
}
