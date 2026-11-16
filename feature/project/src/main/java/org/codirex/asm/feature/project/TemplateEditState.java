package org.codirex.asm.feature.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codirex.asm.domain.template.TemplateFile;

/**
 * Everything the template editor shows: the working copy of one custom template. Immutable.
 * {@link #failure()} is a one-time message, like in the other screens.
 */
final class TemplateEditState {

  enum Phase {
    /** The template is being read. */
    LOADING,
    /** The template could not be found or read. */
    MISSING,
    /** The user is changing it. */
    EDITING,
    /** It is being stored. */
    SAVING,
    /** It was stored: the screen closes. */
    SAVED
  }

  private final Phase phase;
  private final String name;
  private final List<TemplateFile> files;
  private final TemplateFailure failure;

  private TemplateEditState(
      Phase phase, String name, List<TemplateFile> files, TemplateFailure failure) {
    this.phase = phase;
    this.name = name;
    this.files = Collections.unmodifiableList(new ArrayList<>(files));
    this.failure = failure;
  }

  static TemplateEditState loading() {
    return new TemplateEditState(Phase.LOADING, "", Collections.<TemplateFile>emptyList(), null);
  }

  static TemplateEditState missing(TemplateFailure failure) {
    return new TemplateEditState(
        Phase.MISSING, "", Collections.<TemplateFile>emptyList(), failure);
  }

  static TemplateEditState editing(String name, List<TemplateFile> files) {
    return new TemplateEditState(Phase.EDITING, name, files, null);
  }

  TemplateEditState withName(String newName) {
    return new TemplateEditState(phase, newName, files, failure);
  }

  TemplateEditState withFiles(List<TemplateFile> newFiles) {
    return new TemplateEditState(phase, name, newFiles, failure);
  }

  TemplateEditState saving() {
    return new TemplateEditState(Phase.SAVING, name, files, null);
  }

  TemplateEditState saved() {
    return new TemplateEditState(Phase.SAVED, name, files, null);
  }

  /** Back to editing, with a failure to show. */
  TemplateEditState failed(TemplateFailure reason) {
    return new TemplateEditState(Phase.EDITING, name, files, reason);
  }

  TemplateEditState failureHandled() {
    return new TemplateEditState(phase, name, files, null);
  }

  Phase phase() {
    return phase;
  }

  String name() {
    return name;
  }

  List<TemplateFile> files() {
    return files;
  }

  TemplateFailure failure() {
    return failure;
  }
}
