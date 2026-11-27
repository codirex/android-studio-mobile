package org.codirex.asm.domain.xmlassist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * What the libraries of a module add to its layouts: the {@code app:} attributes (read from the
 * values files of the libraries' resources) and the views (classes whose simple name is a styleable,
 * found in the libraries' jars). The Gradle sync lists the resource folders and jars of a module in
 * {@code .asm/libs.txt}, one {@code res <folder>} or {@code jar <file>} per line (ADR-0038).
 */
public final class LibraryIndex {

  private static final LibraryIndex EMPTY =
      new LibraryIndex(AttrCatalog.empty(), Collections.<String>emptySet());

  private final AttrCatalog attributes;
  private final Set<String> views;

  LibraryIndex(AttrCatalog attributes, Set<String> views) {
    this.attributes = attributes;
    this.views = views;
  }

  public static LibraryIndex empty() {
    return EMPTY;
  }

  /** The attributes of the libraries. */
  public AttrCatalog attributes() {
    return attributes;
  }

  /** The fully qualified names of the views of the libraries, sorted. */
  public Set<String> views() {
    return views;
  }

  public boolean isEmpty() {
    return attributes.isEmpty() && views.isEmpty();
  }

  /** The list file of a module, or null if the module was not synced. */
  public static File listOf(File moduleDirectory) {
    File file = new File(moduleDirectory, ".asm/libs.txt");
    return file.isFile() ? file : null;
  }

  /** Reads the list of a module; an unreadable file or entry is skipped, never fatal. */
  public static LibraryIndex load(File list) {
    List<File> resources = new ArrayList<>();
    List<File> jars = new ArrayList<>();
    try (BufferedReader in =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(list), StandardCharsets.UTF_8))) {
      String line;
      while ((line = in.readLine()) != null) {
        if (line.startsWith("res ")) {
          resources.add(new File(line.substring(4)));
        } else if (line.startsWith("jar ")) {
          jars.add(new File(line.substring(4)));
        }
      }
    } catch (IOException e) {
      return EMPTY;
    }
    List<AttrCatalog> catalogs = new ArrayList<>();
    for (File res : resources) {
      File[] files = new File(res, "values").listFiles();
      if (files == null) {
        continue;
      }
      for (File file : files) {
        if (!file.getName().endsWith(".xml")) {
          continue;
        }
        try (InputStream stream = new FileInputStream(file)) {
          catalogs.add(AttrCatalog.parseLibrary(stream));
        } catch (IOException | RuntimeException e) {
          // One broken values file does not stop the others.
        }
      }
    }
    AttrCatalog merged = AttrCatalog.merge(catalogs);
    return new LibraryIndex(merged, findViews(merged, jars));
  }

  /** The base name a styleable stands for: {@code ConstraintLayout_Layout} is ConstraintLayout's. */
  static String viewOf(String styleable) {
    return styleable.endsWith("_Layout")
        ? styleable.substring(0, styleable.length() - "_Layout".length())
        : styleable;
  }

  private static Set<String> findViews(AttrCatalog catalog, List<File> jars) {
    Set<String> wanted = new TreeSet<>();
    for (String styleable : catalog.styleableNames()) {
      String name = viewOf(styleable);
      if (name.indexOf('_') < 0
          && !name.equals("View")
          && !name.startsWith("Base")
          && !ViewClasses.knownTags().contains(name)) {
        wanted.add(name);
      }
    }
    Set<String> found = new TreeSet<>();
    if (wanted.isEmpty()) {
      return found;
    }
    for (File jar : jars) {
      if (!jar.isFile()) {
        continue;
      }
      try (ZipFile zip = new ZipFile(jar)) {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
          String entry = entries.nextElement().getName();
          if (!entry.endsWith(".class") || entry.indexOf('$') >= 0) {
            continue;
          }
          int slash = entry.lastIndexOf('/');
          String simple = entry.substring(slash + 1, entry.length() - ".class".length());
          if (slash > 0 && wanted.contains(simple)) {
            found.add(entry.substring(0, entry.length() - ".class".length()).replace('/', '.'));
          }
        }
      } catch (IOException | RuntimeException e) {
        // A damaged jar only costs its own views.
      }
    }
    return found;
  }
}
