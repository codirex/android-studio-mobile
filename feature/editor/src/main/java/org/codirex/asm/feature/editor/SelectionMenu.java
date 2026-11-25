package org.codirex.asm.feature.editor;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * The menu above a text selection: Cut, Copy, Paste and Select all, and under them the actions of
 * the language server (Definition, Info, Format) when the file's server is ready. It replaces the
 * editor's own selection window so the two sets of actions live in one place.
 *
 * <p>The window does not take focus, so the editor keeps the keyboard and the selection.
 */
final class SelectionMenu {

  /** What the menu items do. All run on the main thread. */
  interface Actions {
    void cut();

    void copy();

    void paste();

    void selectAll();

    void definition();

    void info();

    void format();
  }

  private final PopupWindow window;
  private final View content;
  private final View codeRow;

  SelectionMenu(Context context, Actions actions) {
    content = LayoutInflater.from(context).inflate(R.layout.popup_selection_menu, null, false);
    codeRow = content.findViewById(R.id.menu_code_row);
    bind(R.id.menu_cut, actions::cut);
    bind(R.id.menu_copy, actions::copy);
    bind(R.id.menu_paste, actions::paste);
    bind(R.id.menu_select_all, actions::selectAll);
    bind(R.id.menu_definition, actions::definition);
    bind(R.id.menu_info, actions::info);
    bind(R.id.menu_format, actions::format);
    window =
        new PopupWindow(
            content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
    window.setOutsideTouchable(false);
    window.setClippingEnabled(true);
  }

  private void bind(int id, Runnable action) {
    content
        .findViewById(id)
        .setOnClickListener(
            v -> {
              // The action may change the selection; the menu is done either way.
              dismiss();
              action.run();
            });
  }

  boolean isShowing() {
    return window.isShowing();
  }

  void dismiss() {
    if (window.isShowing()) {
      window.dismiss();
    }
  }

  /**
   * Shows the menu above the selection that starts at {@code line}/{@code column} (below it when
   * there is no room above).
   *
   * @param codeActions whether to show Definition, Info and Format
   */
  void show(CodeEditor editor, int line, int column, boolean codeActions) {
    codeRow.setVisibility(codeActions ? View.VISIBLE : View.GONE);
    content.measure(
        View.MeasureSpec.makeMeasureSpec(editor.getWidth(), View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(editor.getHeight(), View.MeasureSpec.AT_MOST));
    int menuWidth = content.getMeasuredWidth();
    int menuHeight = content.getMeasuredHeight();
    int[] origin = new int[2];
    editor.getLocationInWindow(origin);

    float charX = editor.getCharOffsetX(line, column);
    float charBottom = editor.getCharOffsetY(line, column);
    int rowHeight = editor.getRowHeight();
    int x = Math.round(charX) - menuWidth / 2;
    x = Math.max(0, Math.min(x, editor.getWidth() - menuWidth));
    // Above the first selected row; below the row when that would leave the editor.
    int above = Math.round(charBottom) - rowHeight - menuHeight;
    int y = above >= 0 ? above : Math.round(charBottom) + rowHeight / 2;
    y = Math.max(0, Math.min(y, editor.getHeight() - menuHeight));

    window.setWidth(menuWidth);
    window.setHeight(menuHeight);
    if (window.isShowing()) {
      window.update(origin[0] + x, origin[1] + y, menuWidth, menuHeight);
    } else {
      window.showAtLocation(editor, Gravity.TOP | Gravity.START, origin[0] + x, origin[1] + y);
    }
  }
}
