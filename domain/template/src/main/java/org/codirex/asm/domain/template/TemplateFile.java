package org.codirex.asm.domain.template;

import java.util.Objects;

/**
 * One text file of a template: a path relative to the project root, with {@code /} as separator,
 * and its content. Both may contain placeholders until the template is rendered. Immutable.
 */
public final class TemplateFile {

  private final String path;
  private final String content;

  public TemplateFile(String path, String content) {
    this.path = Objects.requireNonNull(path, "path");
    this.content = Objects.requireNonNull(content, "content");
  }

  public String path() {
    return path;
  }

  public String content() {
    return content;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof TemplateFile)) {
      return false;
    }
    TemplateFile that = (TemplateFile) other;
    return path.equals(that.path) && content.equals(that.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, content);
  }

  @Override
  public String toString() {
    return "TemplateFile(" + path + ")";
  }
}
