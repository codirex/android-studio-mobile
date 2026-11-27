package org.codirex.asm.domain.xmlassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Proposes what fits at the caret of an Android layout file: view tags, {@code android:}
 * attributes of the tag (by its class and by the layout it is in) and the values of an attribute.
 * It knows only the framework (attrs.xml); {@code app:} attributes and project resources are left to
 * the language server.
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

  public AndroidXmlCompleter(AttrCatalog catalog) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
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
    if (typed.indexOf('.') >= 0) {
      return out;
    }
    for (String tag : ViewClasses.knownTags()) {
      if (matches(tag, typed)) {
        out.add(new Proposal(tag, "view", tag, typed.length()));
      }
    }
    return out;
  }

  private List<AttrDef> attributesOf(XmlContext context) {
    List<String> styleables = new ArrayList<>(ViewClasses.chain(context.tag()));
    styleables.addAll(ViewClasses.layoutStyleables(context.parent()));
    return catalog.attributes(styleables);
  }

  private List<Proposal> attributeNames(XmlContext context) {
    List<Proposal> out = new ArrayList<>();
    String prefix = context.prefix();
    String typed = prefix;
    if (prefix.indexOf(':') >= 0) {
      if (!prefix.startsWith("android:")) {
        return out;
      }
      typed = prefix.substring("android:".length());
    }
    for (AttrDef def : attributesOf(context)) {
      String full = "android:" + def.name();
      if (context.existing().contains(full) || !matches(def.name(), typed)) {
        continue;
      }
      out.add(new Proposal(full, join(def.formats()), full + "=\"\"", prefix.length()));
    }
    return out;
  }

  private List<Proposal> attributeValues(XmlContext context) {
    List<Proposal> out = new ArrayList<>();
    String attribute = context.attribute();
    if (!attribute.startsWith("android:")) {
      return out;
    }
    String name = attribute.substring("android:".length());
    AttrDef def = null;
    for (AttrDef candidate : attributesOf(context)) {
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
    String token = bar >= 0 && def.formats().contains("flag") ? typed.substring(bar + 1) : typed;
    List<String> options = new ArrayList<>(def.values());
    if (def.formats().contains("boolean")) {
      options.add("true");
      options.add("false");
    }
    if (!def.formats().contains("flag")
        && (def.formats().contains("reference") || def.formats().contains("color"))
        && (token.isEmpty() || token.startsWith("@"))) {
      options.addAll(REFERENCES);
    }
    for (String option : options) {
      if (!option.toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT))) {
        continue;
      }
      String insert = context.quoted() ? option : "\"" + option + "\"";
      out.add(new Proposal(option, name, insert, token.length()));
    }
    return out;
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
