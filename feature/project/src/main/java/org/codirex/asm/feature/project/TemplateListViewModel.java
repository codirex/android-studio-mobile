package org.codirex.asm.feature.project;

import androidx.lifecycle.LiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.codirex.asm.core.concurrency.RealmFactory;
import org.codirex.asm.core.concurrency.RealmViewModel;
import org.codirex.asm.core.concurrency.UiStateHolder;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.domain.template.Template;
import org.codirex.asm.domain.template.TemplateCatalog;
import org.codirex.asm.domain.template.TemplateManager;

/**
 * Loads the templates, duplicates and deletes custom ones, off the main thread. All methods are
 * called on the main thread.
 */
@HiltViewModel
final class TemplateListViewModel extends RealmViewModel {

  private final TemplateCatalog catalog;
  private final TemplateManager manager;
  private final UiStateHolder<TemplateListState> state =
      new UiStateHolder<>(TemplateListState.initial());

  /** Bumped by every refresh; a result from an older refresh is ignored. */
  private int generation;

  @Inject
  TemplateListViewModel(RealmFactory factory, TemplateCatalog catalog, TemplateManager manager) {
    super(factory);
    this.catalog = catalog;
    this.manager = manager;
  }

  LiveData<TemplateListState> state() {
    return state.observable();
  }

  /** Reloads the list: for every project type its built-in templates, then the custom ones. */
  void refresh() {
    final int mine = ++generation;
    state.set(state.get().withLoading());
    this.<List<TemplateListState.Row>>runAsync(
        checkpoint -> {
          List<TemplateListState.Row> rows = new ArrayList<>();
          for (ProjectType type : ProjectType.values()) {
            for (Template template : catalog.templatesFor(type)) {
              rows.add(new TemplateListState.Row(template));
            }
          }
          return rows;
        },
        outcome -> {
          if (mine != generation) {
            return;
          }
          if (outcome.isSuccess()) {
            state.set(state.get().loaded(outcome.valueOrNull()));
          } else if (outcome.isFailure()) {
            state.set(state.get().failed(TemplateFailure.of(outcome.failureOrNull())));
          }
        });
  }

  /** Copies {@code source} into a new custom template named {@code name}, then reloads. */
  void duplicate(Template source, String name) {
    this.<Boolean>runAsync(
        checkpoint -> {
          manager.duplicate(source, name);
          return Boolean.TRUE;
        },
        outcome -> {
          if (outcome.isFailure()) {
            state.set(state.get().failed(TemplateFailure.of(outcome.failureOrNull())));
          }
          refresh();
        });
  }

  /** Deletes a custom template, then reloads the list whatever happened. */
  void delete(Template template) {
    this.<Boolean>runAsync(
        checkpoint -> {
          manager.delete(template.id());
          return Boolean.TRUE;
        },
        outcome -> {
          if (outcome.isFailure()) {
            state.set(state.get().failed(TemplateFailure.of(outcome.failureOrNull())));
          }
          refresh();
        });
  }

  /** The screen showed the failure; do not show it again. */
  void failureHandled() {
    state.set(state.get().failureHandled());
  }
}
