package org.codirex.asm.domain.xmlassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Proposes what fits at the caret of an Android layout file: view tags, {@code android:}
 * attributes of the tag (by its class and by the layout it is in) and the values of an attribute,
 * including {@code @type/name} references to the resources of the project. It knows the framework
 * (attrs.xml), the project's own resources and, once the module is synced, the {@code app:}
 * attributes and the views of its libraries (ADR-0038).
 */
public final class AndroidXmlCompleter {

  private static final List<String> REFERENCES =
      Arrays.asList(
          "@string/", "@color/", "@drawable/", "@dimen/", "@id/", "@+id/", "@style/", "@layout/");

  /** One proposal, with the number of characters before the caret that it replaces. */
  public static final class Proposal {
    private final String label;
    private final String detail;
    private final String insertText;
    private final int replaceLength;

    Proposal(String label, String detail, String insertText, int replaceLength) {
      this.label = Objects.requireNonNull(label);
      this.detail = Objects.requireNonNull(detail);
      this.insertText = Objects.requireNonNull(insertText);
      this.replaceLength = replaceLength;
    }

    public String label() {
      return label;
    }

    /** The formats of an attribute, or the attribute a value belongs to. */
    public String detail() {
      return detail;
    }

    public String insertText() {
      return insertText;
    }

    /** How many characters before the caret the insert text replaces. */
    public int replaceLength() {
      return replaceLength;
    }
  }

  private final AttrCatalog catalog;
  private final ResourceIndex resources;
  private final LibraryIndex libraries;

  public AndroidXmlCompleter(AttrCatalog catalog) {
    this(catalog, ResourceIndex.empty());
  }

  public AndroidXmlCompleter(AttrCatalog catalog, ResourceIndex resources) {
    this(catalog, resources, LibraryIndex.empty());
  }

  public AndroidXmlCompleter(
      AttrCatalog catalog, ResourceIndex resources, LibraryIndex libraries) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.resources = Objects.requireNonNull(resources, "resources");
    this.libraries = Objects.requireNonNull(libraries, "libraries");
  }

  /** Whether a project file is a layout, where this completion applies. */
  public static boolean appliesTo(String relativePath) {
    String path = "/" + relativePath.replace('\\', '/');
    int res = path.indexOf("/res/layout");
    if (res < 0 || !path.endsWith(".xml")) {
      return false;
    }
    int slash = path.indexOf('/', res + 5);
    return slash > 0 && path.substring(res + 5, slash).startsWith("layout");
  }

  /** The character index of a position (zero based line and column) in a text. */
  public static int offsetOf(String text, int line, int column) {
    int offset = 0;
    for (int i = 0; i < line; i++) {
      int newline = text.indexOf('\n', offset);
      if (newline < 0) {
        return text.length();
      }
      offset = newline + 1;
    }
    return Math.min(offset + column, text.length());
  }

  /** The proposals at {@code offset} of {@code text}; empty where nothing fits. */
  public List<Proposal> complete(String text, int offset) {
    XmlContext context = XmlContext.at(text, offset);
    switch (context.kind()) {
      case TAG_NAME:
        return tags(context);
      case ATTRIBUTE_NAME:
        return attributeNames(context);
      case ATTRIBUTE_VALUE:
        return attributeValues(context);
      default:
        return Collections.emptyList();
    }
  }

  private static boolean matches(String candidate, String typed) {
    return typed.isEmpty()
        || candidate.toLowerCase(Locale.ROOT).contains(typed.toLowerCase(Locale.ROOT));
  }

  private List<Proposal> tags(XmlContext context) {
    List<Proposal> out = new ArrayList<>();
    String typed = context.prefix();
    if (typed.indexOf('.') < 0) {
      for (String tag : ViewClasses.knownTags()) {
        if (matches(tag, typed)) {
          out.add(new Proposal(tag, "view", tag, typed.length()));
        }
      }
    }
    for (String view : libraries.views()) {
      int dot = view.lastIndexOf('.');
      String simple = view.substring(dot + 1);
      boolean fits =
          typed.indexOf('.') >= 0
              ? view.toLowerCase(Locale.ROOT).contains(typed.toLowerCase(Locale.ROOT))
              : matches(simple, typed);
      if (fits) {
        out.add(new Proposal(simple, view.substring(0, Math.max(dot, 0)), view, typed.length()));
      }
    }
    return out;
  }

  private List<AttrDef> attributesOf(XmlContext context) {
    List<String> styleables = new ArrayList<>(ViewClasses.chain(context.tag()));
    styleables.addAll(ViewClasses.layoutStyleables(context.parent()));
    return catalog.attributes(styleables);
  }

  /**
   * The styleables of the libraries that a tag takes: its own (by its simple name), the AppCompat
   * and Material versions of a framework widget, the layout parameters of the parent.
   */
  private List<AttrDef> libraryAttributesOf(XmlContext context) {
    AttrCatalog libs = libraries.attributes();
    if (libs.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> names = new ArrayList<>();
    String simple = context.tag().substring(context.tag().lastIndexOf('.') + 1);
    names.add(simple);
    if (!libs.hasStyleable(simple)) {
      names.add(simple + "_Layout");
    }
    for (String cls : ViewClasses.chain(context.tag())) {
      names.add(cls);
      names.add("AppCompat" + cls);
      names.add("Material" + cls);
    }
    if (context.parent() != null) {
      String parent = context.parent().substring(context.parent().lastIndexOf('.') + 1);
      names.add(parent + "_Layout");
      for (String cls : ViewClasses.chain(context.parent())) {
        names.add(cls + "_Layout");
      }
    }
    return libs.attributes(names);
  }

  private List<Proposal> attributeNames(XmlContext context) {
    List<Proposal> out = new ArrayList<>();
    String prefix = context.prefix();
    boolean framework = true;
    boolean library = true;
    String typed = prefix;
    int colon = prefix.indexOf(':');
    if (colon >= 0) {
      framework = prefix.startsWith("android:");
      library = prefix.startsWith("app:");
      if (!framework && !library) {
        return out;
      }
      typed = prefix.substring(colon + 1);
    }
    if (framework) {
      add(out, attributesOf(context), "android:", context, prefix, typed);
    }
    if (library) {
      add(out, libraryAttributesOf(context), "app:", context, prefix, typed);
    }
    return out;
  }

  private void add(
      List<Proposal> out,
      List<AttrDef> defs,
      String namespace,
      XmlContext context,
      String prefix,
      String typed) {
    for (AttrDef def : defs) {
      String full = namespace + def.name();
      if (context.existing().contains(full)
          || !(matches(def.name(), typed) || full.startsWith(prefix))) {
        continue;
      }
      out.add(new Proposal(full, join(def.formats()), full + "=\"\"", prefix.length()));
    }
  }

  private List<Proposal> attributeValues(XmlContext context) {
    List<Proposal> out = new ArrayList<>();
    String attribute = context.attribute();
    boolean app = attribute.startsWith("app:");
    if (!attribute.startsWith("android:") && !app) {
      return out;
    }
    String name = attribute.substring(attribute.indexOf(':') + 1);
    AttrDef def = null;
    for (AttrDef candidate : app ? libraryAttributesOf(context) : attributesOf(context)) {
      if (candidate.name().equals(name)) {
        def = candidate;
        break;
      }
    }
    if (def == null) {
      return out;
    }
    String typed = context.prefix();
    int bar = typed.lastIndexOf('|');
    boolean flag = def.formats().contains("flag");
    String token = bar >= 0 && flag ? typed.substring(bar + 1) : typed;
    List<String> options = new ArrayList<>(def.values());
    if (def.formats().contains("boolean")) {
      options.add("true");
      options.add("false");
    }
    for (String option : options) {
      if (option.toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT))) {
        out.add(value(option, name, context.quoted(), token.length()));
      }
    }
    boolean accepts =
        def.formats().contains("reference")
            || def.formats().contains("color")
            || def.formats().contains("string")
            || def.formats().contains("dimension");
    if (!flag && accepts && (token.isEmpty() || token.startsWith("@"))) {
      references(token, name, context.quoted(), out);
    }
    return out;
  }

  private static Proposal value(String text, String attribute, boolean quoted, int replace) {
    return new Proposal(text, attribute, quoted ? text : "\"" + text + "\"", replace);
  }

  /** {@code @type/} roots before the slash, the names of the project's resources after it. */
  private void references(String token, String attribute, boolean quoted, List<Proposal> out) {
    int slash = token.indexOf('/');
    if (slash < 0) {
      Set<String> roots = new LinkedHashSet<>(REFERENCES);
      for (String type : resources.types()) {
        roots.add("@" + type + "/");
      }
      for (String root : roots) {
        if (root.toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT))) {
          out.add(value(root, attribute, quoted, token.length()));
        }
      }
      return;
    }
    String head = token.substring(0, slash + 1);
    String type = token.substring(token.startsWith("@+") ? 2 : 1, slash);
    if (type.indexOf(':') >= 0) {
      return;
    }
    String typed = token.substring(slash + 1).toLowerCase(Locale.ROOT);
    for (String resource : resources.names(type)) {
      if (resource.toLowerCase(Locale.ROOT).startsWith(typed)) {
        out.add(value(head + resource, type, quoted, token.length()));
      }
    }
  }

  private static String join(List<String> parts) {
    StringBuilder sb = new StringBuilder();
    for (String part : parts) {
      if (sb.length() > 0) {
        sb.append('|');
      }
      sb.append(part);
    }
    return sb.toString();
  }
}
