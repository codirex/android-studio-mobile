package org.codirex.asm.domain.editor;

import java.util.Locale;

/**
 * The languages the editor knows how to highlight. A file's language comes from its name only; the
 * content is never inspected.
 */
public enum SourceLanguage {
  /** Anything the editor has no support for. Shown without highlighting. */
  PLAIN,
  JAVA,
  /** Kotlin sources and scripts, including {@code build.gradle.kts}. */
  KOTLIN,
  /** Groovy sources, including {@code build.gradle}. */
  GROOVY,
  /** XML files: layouts, manifests, resources. */
  XML,
  JSON,
  MARKDOWN,
  HTML,
  JAVASCRIPT,
  CSS,
  PYTHON,
  /** Shell scripts. */
  SHELL,
  /** {@code .properties} and {@code .ini} style files. */
  PROPERTIES,
  YAML,
  /** C sources and headers ({@code .h} is C). */
  C,
  /** C++ sources and headers. */
  CPP;

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
      case "kt":
      case "kts":
        return KOTLIN;
      case "gradle":
      case "groovy":
        return GROOVY;
      case "xml":
      case "svg":
        return XML;
      case "json":
        return JSON;
      case "md":
      case "markdown":
        return MARKDOWN;
      case "html":
      case "htm":
        return HTML;
      case "js":
      case "mjs":
      case "cjs":
        return JAVASCRIPT;
      case "css":
        return CSS;
      case "py":
        return PYTHON;
      case "sh":
      case "bash":
        return SHELL;
      case "properties":
      case "ini":
        return PROPERTIES;
      case "yaml":
      case "yml":
        return YAML;
      case "c":
      case "h":
        return C;
      case "cpp":
      case "cc":
      case "cxx":
      case "hpp":
      case "hh":
      case "hxx":
        return CPP;
      default:
        return PLAIN;
    }
  }
}
