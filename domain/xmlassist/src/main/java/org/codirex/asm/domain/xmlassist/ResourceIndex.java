package org.codirex.asm.domain.xmlassist;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The resources of a project by type (string, color, drawable, layout, id...): the names declared in
 * {@code res/values*}, the file names in the other {@code res} folders, and the ids that layouts
 * use. It reads every {@code src/main/res} below the project folder and skips tool folders and Gradle
 * build output. Reading is done in one go; scan again to see changes.
 */
public final class ResourceIndex {

  private static final List<String> SKIPPED =
      Arrays.asList(".git", ".gradle", ".idea", ".asm", ".cxx", "node_modules");
  private static final Pattern ID = Pattern.compile("@\\+?id/([A-Za-z_][A-Za-z0-9_]*)");
  private static final int MAX_DEPTH = 8;
  private static final int MAX_TEXT_BYTES = 1000000;

  private final Map<String, TreeSet<String>> byType = new TreeMap<>();

  private ResourceIndex() {}

  /** An index without resources. */
  public static ResourceIndex empty() {
    return new ResourceIndex();
  }

  /** Reads the resources of the project in {@code root}. */
  public static ResourceIndex scan(File root) {
    ResourceIndex index = new ResourceIndex();
    List<File> resources = new ArrayList<>();
    findResourceFolders(root, 0, resources);
    for (File folder : resources) {
      index.read(folder);
    }
    return index;
  }

  /** The types that have at least one name, sorted. */
  public Set<String> types() {
    return Collections.unmodifiableSet(byType.keySet());
  }

  /** The names of a type, sorted; empty for an unknown type. */
  public Set<String> names(String type) {
    TreeSet<String> names = byType.get(type);
    return names == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(names);
  }

  private void add(String type, String name) {
    TreeSet<String> names = byType.get(type);
    if (names == null) {
      names = new TreeSet<>();
      byType.put(type, names);
    }
    names.add(name);
  }

  private static boolean isBuildOutput(File dir) {
    if (!dir.getName().equals("build")) {
      return false;
    }
    File parent = dir.getParentFile();
    return parent != null
        && (new File(parent, "build.gradle").exists() || new File(parent, "build.gradle.kts").exists());
  }

  private static void findResourceFolders(File dir, int depth, List<File> out) {
    File[] children = dir.listFiles();
    if (children == null || depth > MAX_DEPTH) {
      return;
    }
    Arrays.sort(children);
    for (File child : children) {
      if (!child.isDirectory() || SKIPPED.contains(child.getName()) || isBuildOutput(child)) {
        continue;
      }
      File parent = child.getParentFile();
      if (child.getName().equals("res") && parent != null && parent.getName().equals("main")) {
        out.add(child);
      } else {
        findResourceFolders(child, depth + 1, out);
      }
    }
  }

  private void read(File resourceFolder) {
    File[] folders = resourceFolder.listFiles();
    if (folders == null) {
      return;
    }
    for (File folder : folders) {
      if (!folder.isDirectory()) {
        continue;
      }
      String folderName = folder.getName();
      int dash = folderName.indexOf('-');
      String type = dash < 0 ? folderName : folderName.substring(0, dash);
      File[] files = folder.listFiles();
      if (files == null) {
        continue;
      }
      for (File file : files) {
        if (!file.isFile()) {
          continue;
        }
        String fileName = file.getName();
        if (type.equals("values")) {
          if (fileName.endsWith(".xml")) {
            readValues(file);
          }
          continue;
        }
        int dot = fileName.indexOf('.');
        if (dot <= 0) {
          continue;
        }
        add(type, fileName.substring(0, dot));
        if (fileName.endsWith(".xml")
            && (type.equals("layout") || type.equals("menu") || type.equals("navigation"))) {
          readIds(file);
        }
      }
    }
  }

  private static String typeOf(Element element) {
    String tag = element.getNodeName();
    switch (tag) {
      case "string":
      case "color":
      case "dimen":
      case "style":
      case "integer":
      case "bool":
      case "plurals":
      case "attr":
        return tag;
      case "string-array":
      case "integer-array":
      case "array":
        return "array";
      case "item":
        String type = element.getAttribute("type");
        return type.isEmpty() ? null : type;
      default:
        return null;
    }
  }

  private void readValues(File file) {
    try (InputStream in = new FileInputStream(file)) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      Document document = factory.newDocumentBuilder().parse(in);
      for (Node node = document.getDocumentElement().getFirstChild();
          node != null;
          node = node.getNextSibling()) {
        if (!(node instanceof Element)) {
          continue;
        }
        Element element = (Element) node;
        String name = element.getAttribute("name");
        String type = typeOf(element);
        if (!name.isEmpty() && type != null) {
          add(type, name);
        }
      }
    } catch (Exception e) {
      // A file being edited may not be well formed; its names are simply missing for now.
    }
  }

  private void readIds(File file) {
    try (InputStream in = new FileInputStream(file)) {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) > 0 && bytes.size() < MAX_TEXT_BYTES) {
        bytes.write(buffer, 0, read);
      }
      Matcher matcher = ID.matcher(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
      while (matcher.find()) {
        add("id", matcher.group(1));
      }
    } catch (IOException e) {
      // Unreadable: no ids from this file.
    }
  }
}
