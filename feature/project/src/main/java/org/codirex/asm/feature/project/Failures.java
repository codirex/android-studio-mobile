package org.codirex.asm.feature.project;

import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectException.Reason;

/** Turns whatever a background job failed with into a {@link Reason} the screens understand. */
final class Failures {

  private Failures() {}

  static Reason reasonOf(Throwable failure) {
    if (failure instanceof ProjectException) {
      return ((ProjectException) failure).reason();
    }
    return Reason.STORAGE;
  }
}
