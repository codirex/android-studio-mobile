package org.codirex.asm.feature.editor;

import androidx.annotation.NonNull;
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * A proposal that puts the caret between the quotes when it inserts an attribute with an empty
 * value ({@code name=""}), so the value can be typed at once.
 */
final class CaretCompletionItem extends SimpleCompletionItem {

  CaretCompletionItem(CharSequence label, CharSequence desc, int prefixLength, String commitText) {
    super(label, desc, prefixLength, commitText);
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
