package org.codirex.asm.feature.terminal;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.google.android.material.button.MaterialButton;
import org.codirex.asm.terminal.view.KeyEncoder;
import org.codirex.asm.terminal.view.TerminalView;

/**
 * The row of keys that a phone keyboard lacks: Esc, Tab, Ctrl, Alt, arrows and a few symbols.
 * The buttons never take focus, so the keyboard stays open while they are used.
 */
final class ExtraKeys {

  private final TerminalView terminal;
  private final MaterialButton ctrl;
  private final MaterialButton alt;

  ExtraKeys(LinearLayout row, TerminalView terminal) {
    this.terminal = terminal;
    Context context = row.getContext();
    add(row, context.getString(R.string.terminal_key_esc), KeyEncoder.KEY_ESCAPE);
    add(row, context.getString(R.string.terminal_key_tab), KeyEncoder.KEY_TAB);
    ctrl = toggle(row, context.getString(R.string.terminal_key_ctrl));
    alt = toggle(row, context.getString(R.string.terminal_key_alt));
    ctrl.addOnCheckedChangeListener((button, checked) -> terminal.setCtrlLatched(checked));
    alt.addOnCheckedChangeListener((button, checked) -> terminal.setAltLatched(checked));
    add(row, "←", KeyEncoder.KEY_LEFT);
    add(row, "↑", KeyEncoder.KEY_UP);
    add(row, "↓", KeyEncoder.KEY_DOWN);
    add(row, "→", KeyEncoder.KEY_RIGHT);
    add(row, "Home", KeyEncoder.KEY_HOME);
    add(row, "End", KeyEncoder.KEY_END);
    add(row, "PgUp", KeyEncoder.KEY_PAGE_UP);
    add(row, "PgDn", KeyEncoder.KEY_PAGE_DOWN);
    for (String symbol : new String[] {"|", "-", "/", "~"}) {
      addText(row, symbol);
    }
  }

  /** Shows the state of the latches in the toggle buttons (see {@code onModifiersChanged}). */
  void showModifiers(boolean ctrlLatched, boolean altLatched) {
    ctrl.setChecked(ctrlLatched);
    alt.setChecked(altLatched);
  }

  private void add(LinearLayout row, String label, final int key) {
    MaterialButton button = button(row, label);
    button.setOnClickListener(v -> terminal.sendKey(key));
  }

  private void addText(LinearLayout row, final String text) {
    MaterialButton button = button(row, text);
    button.setOnClickListener(v -> terminal.typeText(text));
  }

  private MaterialButton toggle(LinearLayout row, String label) {
    MaterialButton button = button(row, label);
    button.setCheckable(true);
    return button;
  }

  private MaterialButton button(LinearLayout row, String label) {
    MaterialButton button =
        new MaterialButton(
            row.getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
    button.setText(label);
    button.setAllCaps(false);
    button.setFocusable(false);
    button.setFocusableInTouchMode(false);
    button.setMinWidth(0);
    button.setMinimumWidth(0);
    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.setMargins(2, 0, 2, 0);
    row.addView(button, params);
    return button;
  }
}
