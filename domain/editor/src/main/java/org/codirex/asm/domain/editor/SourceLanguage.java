package org.codirex.asm.domain.editor;

import java.util.Locale;

/**
 * The languages the editor knows how to highlight. A file's language comes from its name only; the
 * content is never inspected.
 */
public enum SourceLanguage {
  /** Anything the editor has no support for. Shown without highlighting. */
  PLAIN,
  /** Java source files. */
  JAVA;

  /**
   * The language of the file at {@code path}, chosen by its extension, ignoring case. A name without
   * an extension, or one that only starts with a dot such as {@code .java}, is {@link #PLAIN}.
   */
  public static SourceLanguage forPath(String path) {
    if (path == null) {
      return PLAIN;
    }
    String name = path.substring(path.lastIndexOf('/') + 1);
    int dot = name.lastIndexOf('.');
    if (dot <= 0 || dot == name.length() - 1) {
      return PLAIN;
    }
    String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
    if (extension.equals("java")) {
      return JAVA;
    }
    return PLAIN;
  }
}
