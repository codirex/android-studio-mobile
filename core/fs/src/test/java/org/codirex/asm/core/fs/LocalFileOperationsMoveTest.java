package org.codirex.asm.core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class LocalFileOperationsMoveTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private final FileOperations files = FileOperations.local();

  @Test
  public void movesAFile() throws Exception {
    File source = new File(tmp.getRoot(), "a.txt");
    files.writeText(source, "hello");
    File target = new File(tmp.getRoot(), "b.txt");

    files.move(source, target);

    assertFalse(source.exists());
    assertEquals("hello", files.readText(target));
  }

  @Test
  public void movesAFolderWithItsContent() throws Exception {
    files.writeText(new File(tmp.getRoot(), "src/main/App.java"), "class App {}");
    File target = new File(tmp.getRoot(), "lib");

    files.move(new File(tmp.getRoot(), "src"), target);

    assertFalse(new File(tmp.getRoot(), "src").exists());
    assertEquals("class App {}", files.readText(new File(target, "main/App.java")));
  }

  @Test
  public void neverOverwritesAnExistingTarget() throws Exception {
    File source = new File(tmp.getRoot(), "a.txt");
    File target = new File(tmp.getRoot(), "b.txt");
    files.writeText(source, "A");
    files.writeText(target, "B");

    assertMoveFails(source, target);

    assertEquals("A", files.readText(source));
    assertEquals("B", files.readText(target));
  }

  @Test
  public void aMissingSourceFails() {
    assertMoveFails(new File(tmp.getRoot(), "nope"), new File(tmp.getRoot(), "x"));
  }

  @Test
  public void aMissingTargetFolderFails() throws Exception {
    File source = new File(tmp.getRoot(), "a.txt");
    files.writeText(source, "A");

    assertMoveFails(source, new File(tmp.getRoot(), "missing/b.txt"));

    assertTrue(source.isFile());
  }

  @Test
  public void aFolderCannotMoveIntoItself() throws Exception {
    File folder = new File(tmp.getRoot(), "src");
    files.createDirectories(folder);

    assertMoveFails(folder, new File(folder, "inner"));

    assertTrue(folder.isDirectory());
  }

  private void assertMoveFails(File source, File target) {
    try {
      files.move(source, target);
      fail("expected an IOException");
    } catch (IOException expected) {
      // expected
    }
  }
}
