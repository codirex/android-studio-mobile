package org.codirex.asm.domain.palette;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lists the files of a project for "go to file": paths relative to the project with {@code /}, in
 * folder order. It skips what nobody searches for: version control and tool folders, and the build
 * output of a Gradle module (a folder called build next to a build.gradle file).
 */
public final class FileIndex {

  private static final List<String> SKIPPED =
      Arrays.asList(".git", ".gradle", ".idea", ".asm", ".cxx", "node_modules");
  private static final int MAX_DEPTH = 16;

  private FileIndex() {}

  /** The files below {@code root}, at most {@code limit}. */
  public static List<String> list(File root, int limit) {
    List<String> out = new ArrayList<>();
    walk(root, "", 0, out, limit);
    return out;
  }

  private static void walk(File dir, String prefix, int depth, List<String> out, int limit) {
    File[] children = dir.listFiles();
    if (children == null || depth > MAX_DEPTH) {
      return;
    }
    Arrays.sort(children);
    for (File child : children) {
      if (out.size() >= limit) {
        return;
      }
      String name = child.getName();
      if (child.isDirectory()) {
        if (SKIPPED.contains(name) || isBuildOutput(child)) {
          continue;
        }
        walk(child, prefix + name + "/", depth + 1, out, limit);
      } else if (child.isFile()) {
        out.add(prefix + name);
      }
    }
  }

  private static boolean isBuildOutput(File dir) {
    if (!dir.getName().equals("build")) {
      return false;
    }
    File parent = dir.getParentFile();
    return parent != null
        && (new File(parent, "build.gradle").exists() || new File(parent, "build.gradle.kts").exists());
  }
}
