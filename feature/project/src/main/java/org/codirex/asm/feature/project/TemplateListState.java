package org.codirex.asm.feature.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codirex.asm.domain.template.Template;
import org.codirex.asm.domain.template.TemplateManager;

/**
 * Everything the template list screen shows. Immutable. {@link #failure()} is a one-time message:
 * the screen shows it and then tells the view model it was handled.
 */
final class TemplateListState {

  /** One template of the list. */
  static final class Row {
    private final Template template;
    private final boolean custom;

    Row(Template template) {
      this.template = template;
      this.custom = template.id().startsWith(TemplateManager.ID_PREFIX);
    }

    Template template() {
      return template;
    }

    /** Whether the user made it: only these can be changed or deleted. */
    boolean isCustom() {
      return custom;
    }
  }

  private final List<Row> rows;
  private final boolean loading;
  private final TemplateFailure failure;

  private TemplateListState(List<Row> rows, boolean loading, TemplateFailure failure) {
    this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
    this.loading = loading;
    this.failure = failure;
  }

  static TemplateListState initial() {
    return new TemplateListState(Collections.<Row>emptyList(), true, null);
  }

  TemplateListState withLoading() {
    return new TemplateListState(rows, true, failure);
  }

  TemplateListState loaded(List<Row> newRows) {
    return new TemplateListState(newRows, false, failure);
  }

  TemplateListState failed(TemplateFailure reason) {
    return new TemplateListState(rows, false, reason);
  }

  TemplateListState failureHandled() {
    return new TemplateListState(rows, loading, null);
  }

  List<Row> rows() {
    return rows;
  }

  boolean isLoading() {
    return loading;
  }

  /** A failure not yet shown to the user, or null. */
  TemplateFailure failure() {
    return failure;
  }
}
