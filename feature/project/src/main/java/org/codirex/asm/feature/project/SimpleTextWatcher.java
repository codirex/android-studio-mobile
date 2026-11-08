package org.codirex.asm.feature.project;

import android.text.Editable;
import android.text.TextWatcher;

/** A {@link TextWatcher} that only reports the final text. */
abstract class SimpleTextWatcher implements TextWatcher {

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {}

  @Override
  public abstract void afterTextChanged(Editable s);
}
