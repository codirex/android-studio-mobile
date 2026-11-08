package org.codirex.asm.domain.project;

import java.util.List;
import java.util.Optional;

/**
 * Where projects are kept. The domain defines it; {@code data:project} implements it.
 *
 * <p><b>Every method blocks.</b> Callers run them in a background job, never on the main thread.
 * Implementations are safe to call from several threads.
 */
public interface ProjectRepository {

  /**
   * All projects, most recently created first. A folder that is not a project, or whose metadata is
   * broken, is left out instead of failing the whole listing.
   *
   * @throws ProjectException with reason {@code STORAGE} if the storage cannot be read
   */
  List<Project> list() throws ProjectException;

  /**
   * Finds a project by name.
   *
   * @return the project, or empty if there is none with this name
   * @throws ProjectException with reason {@code INVALID_METADATA} if it exists but cannot be read,
   *     or {@code STORAGE}
   */
  Optional<Project> find(ProjectName name) throws ProjectException;

  /**
   * Creates an empty project: its folder and its metadata. Source files come from templates, which
   * are applied on top of the new project by a later feature.
   *
   * @throws ProjectException with reason {@code ALREADY_EXISTS} if the name is taken, or {@code
   *     STORAGE}. A failed creation leaves nothing behind.
   */
  Project create(ProjectSpec spec) throws ProjectException;

  /**
   * Deletes a project and all its files.
   *
   * @throws ProjectException with reason {@code NOT_FOUND} if there is no such project, or {@code
   *     STORAGE}
   */
  void delete(ProjectName name) throws ProjectException;
}
