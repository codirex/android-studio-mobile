package org.codirex.asm.domain.environment;

import java.io.IOException;
import java.util.List;

/**
 * Keeps the variables. The domain defines it; a data module implements it.
 *
 * <p><b>Every method may block</b> (it reads or writes a file). Callers run it in a background job.
 */
public interface EnvironmentStore {

  /** Every stored variable. Entries that are no longer valid are left out. */
  List<EnvironmentVariable> load() throws IOException;

  /** Replaces what is stored with {@code variables}. */
  void save(List<EnvironmentVariable> variables) throws IOException;
}
