package org.codirex.asm.feature.editor;

import org.codirex.asm.domain.editor.DocumentException;
import org.codirex.asm.domain.editor.DocumentException.Reason;

/** Turns whatever a background job failed with into a {@link Reason} the screen understands. */
final class Failures {

  private Failures() {}

  static Reason reasonOf(Throwable failure) {
    if (failure instanceof DocumentException) {
      return ((DocumentException) failure).reason();
    }
    return Reason.STORAGE;
  }
}
