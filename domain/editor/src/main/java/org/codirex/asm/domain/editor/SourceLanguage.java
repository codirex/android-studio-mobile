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
  JAVA,
  /** XML files: layouts, manifests, resources. */
  XML,
  /** JSON files. */
  JSON,
  /** Markdown documents. */
  MARKDOWN,
  /** Gradle build scripts, Groovy or Kotlin. */
  GRADLE;

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
    switch (name.substring(dot + 1).toLowerCase(Locale.ROOT)) {
      case "java":
        return JAVA;
      case "xml":
        return XML;
      case "json":
        return JSON;
      case "md":
      case "markdown":
        return MARKDOWN;
      case "gradle":
        return GRADLE;
      case "kts":
        // Only the Gradle scripts: a .kts file is a build script in an Android project.
        return name.endsWith(".gradle.kts") ? GRADLE : PLAIN;
      default:
        return PLAIN;
    }
  }
}
