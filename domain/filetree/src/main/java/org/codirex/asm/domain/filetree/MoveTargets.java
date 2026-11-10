package org.codirex.asm.domain.filetree;

import java.util.ArrayList;
import java.util.List;

/**
 * The folders an entry can be moved to. Pure logic, so it is tested on the JVM; the folder list
 * itself comes from {@link ProjectFiles#folders}.
 */
public final class MoveTargets {

  private MoveTargets() {}

  /**
   * The folders to offer for moving {@code sourcePath}: the project root (the empty path) first,
   * then {@code folders} sorted by name ignoring case. Left out are the folder that already holds
   * the entry, and, when the entry is a folder, itself and every folder inside it.
   *
   * @param folders paths of all folders of the project, without the root
   * @param sourcePath the entry to move
   * @param sourceIsDirectory whether the entry is a folder
   */
  public static List<String> of(
      List<String> folders, String sourcePath, boolean sourceIsDirectory) {
    int slash = sourcePath.lastIndexOf('/');
    String parent = slash < 0 ? "" : sourcePath.substring(0, slash);

    List<String> sorted = new ArrayList<>(folders);
    sorted.sort(String.CASE_INSENSITIVE_ORDER);

    List<String> result = new ArrayList<>();
    if (!parent.isEmpty()) {
      result.add("");
    }
    for (String folder : sorted) {
      if (folder.isEmpty() || folder.equals(parent)) {
        continue;
      }
      if (sourceIsDirectory
          && (folder.equals(sourcePath) || folder.startsWith(sourcePath + "/"))) {
        continue;
      }
      result.add(folder);
    }
    return result;
  }
}
