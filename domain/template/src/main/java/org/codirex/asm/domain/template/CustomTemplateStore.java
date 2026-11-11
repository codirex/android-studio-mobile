package org.codirex.asm.domain.template;

import java.util.List;

/**
 * Where the templates the user made are kept. The domain defines it; a data module implements it.
 *
 * <p><b>Every method may block.</b> Callers run it in a background job.
 */
public interface CustomTemplateStore {

  /**
   * All stored templates. A stored template that cannot be read is left out; it must not hide the
   * others.
   *
   * @throws TemplateException with reason {@code STORAGE} if the store itself cannot be read
   */
  List<Template> load() throws TemplateException;

  /** Stores {@code template}, replacing a stored one with the same id. */
  void save(Template template) throws TemplateException;

  /** Removes the template with {@code id}. A missing template is not an error. */
  void delete(String id) throws TemplateException;
}
