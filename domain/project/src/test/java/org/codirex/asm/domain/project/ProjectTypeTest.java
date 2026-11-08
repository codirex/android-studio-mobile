package org.codirex.asm.domain.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public final class ProjectTypeTest {

  @Test
  public void idsRoundTrip() {
    for (ProjectType type : ProjectType.values()) {
      assertEquals(type, ProjectType.fromId(type.id()));
    }
  }

  @Test
  public void idsAreStable() {
    // These ids are stored in templates. Changing one breaks every saved template.
    assertEquals("application", ProjectType.APPLICATION.id());
    assertEquals("android-library", ProjectType.ANDROID_LIBRARY.id());
    assertEquals("java-library", ProjectType.JAVA_LIBRARY.id());
    assertEquals("gradle-plugin", ProjectType.GRADLE_PLUGIN.id());
  }

  @Test
  public void unknownIdThrows() {
    try {
      ProjectType.fromId("nope");
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // ok
    }
  }
}
