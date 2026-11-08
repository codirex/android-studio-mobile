package org.codirex.asm.core.concurrency;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.codirex.clotho.Realm;

/** The production factory: a plain Clotho realm with default policy. */
@Singleton
final class DefaultRealmFactory implements RealmFactory {

  @Inject
  DefaultRealmFactory() {}

  @Override
  public Realm open(String name) {
    return Realm.open(name);
  }
}
