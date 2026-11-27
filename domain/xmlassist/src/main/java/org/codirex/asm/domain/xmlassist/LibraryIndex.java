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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * What the libraries of a module add to its layouts: the {@code app:} attributes (read from the
 * values files of the libraries' resources) and the views (classes whose simple name is a styleable,
 * found in the libraries' jars). The Gradle sync lists the resource folders and jars of a module in
 * {@code .asm/libs.txt}, one {@code res <folder>}, {@code jar <file>} or {@code src <folder>} (Java and
 * Kotlin sources of the project, for its own views) per line (ADR-0038, ADR-0039).
 */
public final class LibraryIndex {

  private static final LibraryIndex EMPTY =
      new LibraryIndex(
          AttrCatalog.empty(),
          Collections.<String>emptySet(),
          Collections.<File>emptyList(),
          Collections.<String>emptySet(),
          Collections.<String, String>emptyMap());

  private final AttrCatalog attributes;
  private final Set<String> libraryViews;
  private final List<File> sources;
  private final Set<String> projectViews;
  private final Map<String, String> superOf;
  private final Set<String> views;

  LibraryIndex(
      AttrCatalog attributes,
      Set<String> libraryViews,
      List<File> sources,
      Set<String> projectViews,
      Map<String, String> superOf) {
    this.attributes = attributes;
    this.libraryViews = libraryViews;
    this.sources = sources;
    this.projectViews = projectViews;
    this.superOf = superOf;
    Set<String> all = new TreeSet<>(libraryViews);
    all.addAll(projectViews);
    this.views = Collections.unmodifiableSet(all);
  }

  public static LibraryIndex empty() {
    return EMPTY;
  }

  /** The attributes of the libraries. */
  public AttrCatalog attributes() {
    return attributes;
  }

  /** The fully qualified names of the views of the libraries and of the project, sorted. */
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
    List<File> sources = new ArrayList<>();
    try (BufferedReader in =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(list), StandardCharsets.UTF_8))) {
      String line;
      while ((line = in.readLine()) != null) {
        if (line.startsWith("res ")) {
          resources.add(new File(line.substring(4)));
        } else if (line.startsWith("jar ")) {
          jars.add(new File(line.substring(4)));
        } else if (line.startsWith("src ")) {
          sources.add(new File(line.substring(4)));
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
    Set<String> libraryViews = findViews(merged, jars);
    return new LibraryIndex(
            merged, libraryViews, sources, Collections.<String>emptySet(), Collections.<String, String>emptyMap())
        .refreshSources();
  }

  /** The same libraries with the views of the project read again from its sources. */
  public LibraryIndex refreshSources() {
    Map<String, String> supers = new HashMap<>();
    Map<String, String> names = new HashMap<>();
    for (File root : sources) {
      scanSources(root, supers, names, 0);
    }
    Set<String> known = new HashSet<>(ViewClasses.knownTags());
    known.add("View");
    for (String view : libraryViews) {
      known.add(view.substring(view.lastIndexOf('.') + 1));
    }
    Set<String> found = new TreeSet<>();
    for (Map.Entry<String, String> e : names.entrySet()) {
      String simple = e.getKey();
      String current = simple;
      int hops = 0;
      while (current != null && hops++ < 20) {
        String parent = supers.get(current);
        if (parent == null) {
          break;
        }
        if (known.contains(parent) || isFrameworkFlavour(parent)) {
          found.add(e.getValue());
          break;
        }
        current = parent;
      }
    }
    return new LibraryIndex(attributes, libraryViews, sources, found, supers);
  }

  /** {@code MaterialButton} and {@code AppCompatButton} are flavours of {@code Button}. */
  private static boolean isFrameworkFlavour(String name) {
    return flavourOf(name) != null;
  }

  private static String flavourOf(String name) {
    for (String prefix : new String[] {"AppCompat", "Material"}) {
      if (name.startsWith(prefix) && ViewClasses.knownTags().contains(name.substring(prefix.length()))) {
        return name.substring(prefix.length());
      }
    }
    return null;
  }

  /**
   * The class and its superclasses, as far as the sources and the libraries tell: {@code [Fancy,
   * MaterialButton]}. The simple name alone for a class that is not known.
   */
  public List<String> chainOf(String simple) {
    List<String> out = new ArrayList<>();
    String current = simple;
    while (current != null && !out.contains(current) && out.size() < 20) {
      out.add(current);
      current = superOf.get(current);
    }
    return out;
  }

  /** The framework tag a class descends from (Button for a MaterialButton); null if unknown. */
  public String frameworkBase(String simple) {
    for (String name : chainOf(simple)) {
      if (name.equals("View") || ViewClasses.knownTags().contains(name)) {
        return name;
      }
      String flavour = flavourOf(name);
      if (flavour != null) {
        return flavour;
      }
    }
    return null;
  }

  private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)");
  private static final Pattern JAVA_CLASS =
      Pattern.compile("\\bclass\\s+(\\w+)(?:\\s*<[^>{]*>)?\\s+extends\\s+([\\w.]+)");
  private static final Pattern KOTLIN_CLASS =
      Pattern.compile("\\bclass\\s+(\\w+)[^{]{0,600}?\\)\\s*:\\s*([\\w.]+)\\s*[(<]");

  private static void scanSources(File dir, Map<String, String> supers, Map<String, String> names, int depth) {
    File[] children = dir.listFiles();
    if (children == null || depth > 12) {
      return;
    }
    for (File child : children) {
      if (child.isDirectory()) {
        scanSources(child, supers, names, depth + 1);
        continue;
      }
      String file = child.getName();
      boolean isJava = file.endsWith(".java");
      if ((!isJava && !file.endsWith(".kt")) || child.length() > 300_000) {
        continue;
      }
      String text;
      try {
        text = new String(java.nio.file.Files.readAllBytes(child.toPath()), StandardCharsets.UTF_8);
      } catch (IOException | RuntimeException e) {
        continue;
      }
      Matcher pkg = PACKAGE.matcher(text);
      String prefix = pkg.find() ? pkg.group(1) + "." : "";
      Matcher cls = (isJava ? JAVA_CLASS : KOTLIN_CLASS).matcher(text);
      while (cls.find()) {
        String parent = cls.group(2);
        supers.put(cls.group(1), parent.substring(parent.lastIndexOf('.') + 1));
        names.put(cls.group(1), prefix + cls.group(1));
      }
    }
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
