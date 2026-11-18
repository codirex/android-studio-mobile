package org.codirex.asm.domain.toolchain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public final class GradleTaskTest {

  @Test
  public void theCommandUsesTheWrapperOfTheProjectWhenThereIsOne() {
    GradleTask task = new GradleTask("assembleDebug");
    assertEquals("sh ./gradlew assembleDebug", task.command(true));
    assertEquals("gradle assembleDebug", task.command(false));
  }

  @Test
  public void taskPathsAreAccepted() {
    assertEquals("gradle :app:assembleDebug", new GradleTask(":app:assembleDebug").command(false));
    assertTrue(GradleTask.isValidName("lint-release"));
    assertTrue(GradleTask.isValidName("publish.to.local"));
  }

  @Test
  public void nothingThatCouldBeShellSyntaxIsAccepted() {
    String[] bad = {"", "-x", "--help", "build clean", "build;ls", "build&&ls", "a|b", "$(id)", "`id`", "a>b", "a\nb", "a'b", "a\"b"};
    for (String name : bad) {
      assertFalse(name, GradleTask.isValidName(name));
      try {
        new GradleTask(name);
        fail("accepted " + name);
      } catch (IllegalArgumentException expected) {
        // expected
      }
    }
  }

  @Test
  public void theCommonTasksAreValidAndDistinct() {
    List<GradleTask> common = GradleTask.common();
    assertEquals("build", common.get(0).name());
    Set<String> names = new HashSet<>();
    for (GradleTask task : common) {
      assertTrue(GradleTask.isValidName(task.name()));
      assertTrue(names.add(task.name()));
    }
  }
}
