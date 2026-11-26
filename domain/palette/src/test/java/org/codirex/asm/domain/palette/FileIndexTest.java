package org.codirex.asm.domain.palette;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileIndexTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private void write(String path) throws IOException {
    File file = new File(folder.getRoot(), path);
    assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
    assertTrue(file.createNewFile());
  }

  @Test
  public void listsRelativePathsAndSkipsToolFoldersAndBuildOutput() throws IOException {
    write("build.gradle");
    write("app/src/Main.java");
    write("build/out/Main.class");
    write(".git/config");
    write(".gradle/cache.bin");
    List<String> files = FileIndex.list(folder.getRoot(), 100);
    assertEquals(Arrays.asList("app/src/Main.java", "build.gradle"), files);
  }

  @Test
  public void aFolderCalledBuildWithoutAGradleFileIsKept() throws IOException {
    write("feature/build/Foo.java");
    assertEquals(Arrays.asList("feature/build/Foo.java"), FileIndex.list(folder.getRoot(), 100));
  }

  @Test
  public void theLimitStopsTheWalk() throws IOException {
    write("a.txt");
    write("b.txt");
    write("c.txt");
    assertEquals(2, FileIndex.list(folder.getRoot(), 2).size());
  }
}
