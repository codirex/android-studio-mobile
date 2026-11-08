package org.codirex.asm.core.concurrency;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.codirex.clotho.Realm;
import org.junit.Test;

public final class DefaultRealmFactoryTest {

  @Test
  public void opensAnOpenRealm() {
    Realm realm = new DefaultRealmFactory().open("test");
    try {
      assertFalse(realm.isClosing());
    } finally {
      realm.closeAsync();
    }
  }

  @Test
  public void closeAsyncMarksTheRealmClosing() {
    Realm realm = new DefaultRealmFactory().open("test");
    realm.closeAsync();
    assertTrue(realm.isClosing());
  }
}
