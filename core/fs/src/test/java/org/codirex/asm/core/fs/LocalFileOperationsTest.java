package org.codirex.asm.core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class LocalFileOperationsTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private final LocalFileOperations ops = new LocalFileOperations();

  @Test
  public void listsFoldersFirstThenFilesIgnoringCase() throws IOException {
    tmp.newFile("beta.txt");
    tmp.newFile("Alpha.txt");
    tmp.newFolder("zeta");
    tmp.newFolder("Gamma");

    List<FileEntry> entries = ops.list(tmp.getRoot());

    assertEquals(4, entries.size());
    assertEquals("Gamma", entries.get(0).name());
    assertEquals("zeta", entries.get(1).name());
    assertEquals("Alpha.txt", entries.get(2).name());
    assertEquals("beta.txt", entries.get(3).name());
    assertTrue(entries.get(0).isDirectory());
    assertFalse(entries.get(2).isDirectory());
  }

  @Test
  public void listingAFileFails() throws IOException {
    File file = tmp.newFile("a.txt");
    try {
      ops.list(file);
      fail("expected IOException");
    } catch (IOException expected) {
      // ok
    }
  }

  @Test
  public void writeThenReadRoundTripsUtf8() throws IOException {
    File file = new File(tmp.getRoot(), "nested/dir/a.txt");
    ops.writeText(file, "héllo مرحبا ✓");
    assertEquals("héllo مرحبا ✓", ops.readText(file));
  }

  @Test
  public void writeReplacesExistingContentAndLeavesNoTempFile() throws IOException {
    File file = tmp.newFile("a.txt");
    ops.writeText(file, "first");
    ops.writeText(file, "second");

    assertEquals("second", ops.readText(file));
    String[] names = tmp.getRoot().list();
    assertEquals(1, names.length);
    assertEquals("a.txt", names[0]);
  }

  @Test
  public void writingOverADirectoryFails() throws IOException {
    File dir = tmp.newFolder("d");
    try {
      ops.writeText(dir, "x");
      fail("expected IOException");
    } catch (IOException expected) {
      // ok
    }
  }

  @Test
  public void createDirectoriesIsIdempotent() throws IOException {
    File dir = new File(tmp.getRoot(), "a/b/c");
    assertEquals(dir, ops.createDirectories(dir));
    assertTrue(dir.isDirectory());
    ops.createDirectories(dir);
  }

  @Test
  public void createDirectoriesFailsOverAFile() throws IOException {
    File file = tmp.newFile("f");
    try {
      ops.createDirectories(file);
      fail("expected IOException");
    } catch (IOException expected) {
      // ok
    }
  }

  @Test
  public void deleteRemovesATree() throws IOException {
    File root = tmp.newFolder("tree");
    File inner = new File(root, "a/b");
    assertTrue(inner.mkdirs());
    assertTrue(new File(inner, "f.txt").createNewFile());

    ops.delete(root);

    assertFalse(root.exists());
  }

  @Test
  public void deletingSomethingMissingSucceeds() throws IOException {
    ops.delete(new File(tmp.getRoot(), "nope"));
  }

  @Test
  public void deleteRemovesASymlinkButNotItsTarget() throws IOException {
    File target = tmp.newFolder("target");
    File keep = new File(target, "keep.txt");
    assertTrue(keep.createNewFile());
    File link = new File(tmp.getRoot(), "link");
    try {
      Files.createSymbolicLink(link.toPath(), target.toPath());
    } catch (IOException | UnsupportedOperationException e) {
      Assume.assumeNoException(e);
    }

    ops.delete(link);

    assertFalse(link.exists());
    assertTrue(keep.exists());
  }

  @Test
  public void resolveStaysInsideTheRoot() throws IOException {
    File resolved = ops.resolve(tmp.getRoot(), "a/../b/c.txt");
    assertEquals(new File(tmp.getRoot().getCanonicalFile(), "b/c.txt"), resolved);
    assertEquals(tmp.getRoot().getCanonicalFile(), ops.resolve(tmp.getRoot(), ""));
  }

  @Test
  public void resolveRejectsEscapes() throws IOException {
    for (String bad : new String[] {"../x", "a/../../x", "/etc/passwd"}) {
      try {
        ops.resolve(tmp.getRoot(), bad);
        fail("expected IOException for " + bad);
      } catch (IOException expected) {
        // ok
      }
    }
  }

  @Test
  public void resolveRejectsASymlinkThatLeavesTheRoot() throws IOException {
    File outside = tmp.newFolder("outside");
    File root = tmp.newFolder("root");
    File link = new File(root, "link");
    try {
      Files.createSymbolicLink(link.toPath(), outside.toPath());
    } catch (IOException | UnsupportedOperationException e) {
      Assume.assumeNoException(e);
    }
    try {
      ops.resolve(root, "link/file.txt");
      fail("expected IOException");
    } catch (IOException expected) {
      // ok
    }
  }

  @Test
  public void resolveDoesNotMatchASiblingWithTheSamePrefix() throws IOException {
    File root = tmp.newFolder("app");
    tmp.newFolder("app-evil");
    try {
      ops.resolve(root, "../app-evil/x");
      fail("expected IOException");
    } catch (IOException expected) {
      // ok
    }
  }
}
