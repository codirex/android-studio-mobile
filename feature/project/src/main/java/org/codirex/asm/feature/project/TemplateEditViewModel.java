package org.codirex.asm.feature.project;

import androidx.lifecycle.LiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.codirex.asm.core.concurrency.RealmFactory;
import org.codirex.asm.core.concurrency.RealmViewModel;
import org.codirex.asm.core.concurrency.UiStateHolder;
import org.codirex.asm.domain.template.Template;
import org.codirex.asm.domain.template.TemplateFile;
import org.codirex.asm.domain.template.TemplateManager;

/**
 * Holds the working copy of one custom template while it is edited, and stores it. All methods are
 * called on the main thread. The variables and the type of the template are not edited here; they
 * are kept as they were.
 */
@HiltViewModel
final class TemplateEditViewModel extends RealmViewModel {

  private final TemplateManager manager;
  private final UiStateHolder<TemplateEditState> state =
      new UiStateHolder<>(TemplateEditState.loading());

  private Template original;
  private boolean started;

  @Inject
  TemplateEditViewModel(RealmFactory factory, TemplateManager manager) {
    super(factory);
    this.manager = manager;
  }

  LiveData<TemplateEditState> state() {
    return state.observable();
  }

  /** Reads the template with {@code id}. Asking again does nothing: the copy being edited stays. */
  void load(String id) {
    if (started) {
      return;
    }
    started = true;
    this.<List<Template>>runAsync(
        checkpoint -> {
          List<Template> found = new ArrayList<>();
          for (Template template : manager.custom()) {
            if (template.id().equals(id)) {
              found.add(template);
            }
          }
          return found;
        },
        outcome -> {
          if (outcome.isSuccess()) {
            List<Template> found = outcome.valueOrNull();
            if (found.isEmpty()) {
              state.set(TemplateEditState.missing(null));
            } else {
              original = found.get(0);
              state.set(TemplateEditState.editing(original.name(), original.files()));
            }
          } else if (outcome.isFailure()) {
            state.set(TemplateEditState.missing(TemplateFailure.of(outcome.failureOrNull())));
          }
        });
  }

  void setName(String name) {
    TemplateEditState current = state.get();
    if (current.phase() == TemplateEditState.Phase.EDITING && !current.name().equals(name)) {
      state.set(current.withName(name));
    }
  }

  /** Replaces the file at {@code index}, or adds {@code file} at the end when index is -1. */
  void putFile(int index, TemplateFile file) {
    TemplateEditState current = state.get();
    if (current.phase() != TemplateEditState.Phase.EDITING) {
      return;
    }
    List<TemplateFile> files = new ArrayList<>(current.files());
    if (index >= 0 && index < files.size()) {
      files.set(index, file);
    } else {
      files.add(file);
    }
    state.set(current.withFiles(files));
  }

  void removeFile(int index) {
    TemplateEditState current = state.get();
    if (current.phase() != TemplateEditState.Phase.EDITING
        || index < 0
        || index >= current.files().size()) {
      return;
    }
    List<TemplateFile> files = new ArrayList<>(current.files());
    files.remove(index);
    state.set(current.withFiles(files));
  }

  /** Stores the working copy. A template that breaks a rule is refused with the reason. */
  void save() {
    final TemplateEditState current = state.get();
    if (current.phase() != TemplateEditState.Phase.EDITING || original == null) {
      return;
    }
    final Template updated =
        new Template(
            original.id(),
            current.name().trim(),
            original.type(),
            original.defaults(),
            current.files());
    state.set(current.saving());
    this.<Boolean>runAsync(
        checkpoint -> {
          manager.save(updated);
          return Boolean.TRUE;
        },
        outcome -> {
          if (outcome.isSuccess()) {
            state.set(state.get().saved());
          } else if (outcome.isFailure()) {
            state.set(state.get().failed(TemplateFailure.of(outcome.failureOrNull())));
          } else {
            state.set(state.get().failed(null));
          }
        });
  }

  /** The screen showed the failure; do not show it again. */
  void failureHandled() {
    state.set(state.get().failureHandled());
  }
}
