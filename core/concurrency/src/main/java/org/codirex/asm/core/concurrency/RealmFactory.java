package org.codirex.asm.core.concurrency;

import org.codirex.clotho.Realm;

/**
 * Opens realms. Inject this instead of calling {@code Realm.open} so tests can substitute a
 * factory and so every realm in the app is created in one place.
 */
public interface RealmFactory {

  /**
   * Opens a new realm.
   *
   * @param name a short name that shows up in diagnostics
   * @return an open realm the caller owns and must close
   */
  Realm open(String name);
}
