package org.codirex.asm.feature.project;

import org.codirex.asm.domain.project.ProjectException.Reason;

/**
 * The create screen's state. Immutable.
 *
 * <p>{@link #created()} and {@link #failure()} are one-time events: the screen reacts and then tells
 * the view model it was handled, so a rotation does not repeat them.
 */
final class CreateProjectState {

  private static final CreateProjectState IDLE = new CreateProjectState(false, false, null);
  private static final CreateProjectState BUSY = new CreateProjectState(true, false, null);
  private static final CreateProjectState CREATED = new CreateProjectState(false, true, null);

  private final boolean busy;
  private final boolean created;
  private final Reason failure;

  private CreateProjectState(boolean busy, boolean created, Reason failure) {
    this.busy = busy;
    this.created = created;
    this.failure = failure;
  }

  static CreateProjectState idle() {
    return IDLE;
  }

  static CreateProjectState busy() {
    return BUSY;
  }

  static CreateProjectState created() {
    return CREATED;
  }

  static CreateProjectState failed(Reason reason) {
    return new CreateProjectState(false, false, reason);
  }

  boolean busy() {
    return busy;
  }

  boolean isCreated() {
    return created;
  }

  /** The reason creation failed, not yet shown, or null. */
  Reason failure() {
    return failure;
  }
}
