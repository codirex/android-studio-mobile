package org.codirex.asm.domain.environment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * The use cases of the custom environment variables (ADR-0028): list them, set one, remove one. The
 * terminal reads them when a session starts, so a change applies to new sessions.
 *
 * <p><b>Every method blocks.</b> Callers run it in a background job.
 */
public final class EnvironmentVariables {

  private static final Comparator<EnvironmentVariable> BY_NAME =
      new Comparator<EnvironmentVariable>() {
        @Override
        public int compare(EnvironmentVariable a, EnvironmentVariable b) {
          return a.name().compareTo(b.name());
        }
      };

  private final EnvironmentStore store;

  public EnvironmentVariables(EnvironmentStore store) {
    this.store = Objects.requireNonNull(store, "store");
  }

  /** The variables, sorted by name. */
  public List<EnvironmentVariable> list() throws EnvironmentException {
    List<EnvironmentVariable> all = new ArrayList<>(load());
    Collections.sort(all, BY_NAME);
    return all;
  }

  /** Adds the variable, or changes its value if the name exists. The name is trimmed. */
  public void set(String name, String value) throws EnvironmentException {
    EnvironmentVariable variable = new EnvironmentVariable(name.trim(), value);
    List<EnvironmentVariable> all = new ArrayList<>(load());
    for (Iterator<EnvironmentVariable> it = all.iterator(); it.hasNext(); ) {
      if (it.next().name().equals(variable.name())) {
        it.remove();
      }
    }
    all.add(variable);
    save(all);
  }

  /** Removes the variable; nothing happens if there is none of that name. */
  public void remove(String name) throws EnvironmentException {
    List<EnvironmentVariable> all = new ArrayList<>(load());
    boolean changed = false;
    for (Iterator<EnvironmentVariable> it = all.iterator(); it.hasNext(); ) {
      if (it.next().name().equals(name)) {
        it.remove();
        changed = true;
      }
    }
    if (changed) {
      save(all);
    }
  }

  private List<EnvironmentVariable> load() throws EnvironmentException {
    try {
      return store.load();
    } catch (IOException e) {
      throw new EnvironmentException(EnvironmentException.Reason.STORAGE, "load", e);
    }
  }

  private void save(List<EnvironmentVariable> all) throws EnvironmentException {
    try {
      store.save(all);
    } catch (IOException e) {
      throw new EnvironmentException(EnvironmentException.Reason.STORAGE, "save", e);
    }
  }
}
