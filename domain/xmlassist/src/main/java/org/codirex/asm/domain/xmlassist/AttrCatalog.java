package org.codirex.asm.domain.xmlassist;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * The attributes of the framework grouped by styleable (View, TextView, LinearLayout_Layout...), read
 * from the attrs.xml of an SDK platform (data/res/values/attrs.xml). An attribute that a styleable
 * mentions without a definition gets the one given elsewhere in the file.
 */
public final class AttrCatalog {

  private final Map<String, List<AttrDef>> styleables;

  private AttrCatalog(Map<String, List<AttrDef>> styleables) {
    this.styleables = styleables;
  }

  /** Reads an attrs.xml. */
  public static AttrCatalog parse(InputStream in) throws IOException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(false);
      factory.setValidating(false);
      try {
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      } catch (ParserConfigurationException ignored) {
        // Not every parser knows the feature; attrs.xml has no doctype anyway.
      }
      Document document = factory.newDocumentBuilder().parse(in);
      return fromDocument(document);
    } catch (ParserConfigurationException | SAXException e) {
      throw new IOException("not a valid attrs.xml", e);
    }
  }

  private static AttrCatalog fromDocument(Document document) {
    Element root = document.getDocumentElement();
    // First every definition, wherever it stands, so that bare mentions can be resolved.
    Map<String, AttrDef> definitions = new HashMap<>();
    collect(root, definitions);
    Map<String, List<AttrDef>> styleables = new LinkedHashMap<>();
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (!(node instanceof Element) || !"declare-styleable".equals(node.getNodeName())) {
        continue;
      }
      Element styleable = (Element) node;
      String name = styleable.getAttribute("name");
      List<AttrDef> list = styleables.get(name);
      if (list == null) {
        list = new ArrayList<>();
        styleables.put(name, list);
      }
      for (Node child = styleable.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element && "attr".equals(child.getNodeName())) {
          AttrDef def = read((Element) child);
          if (def == null) {
            continue;
          }
          if (def.isBare() && definitions.containsKey(def.name())) {
            def = definitions.get(def.name());
          }
          list.add(def);
        }
      }
    }
    return new AttrCatalog(styleables);
  }

  private static void collect(Element parent, Map<String, AttrDef> definitions) {
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (!(node instanceof Element)) {
        continue;
      }
      if ("attr".equals(node.getNodeName())) {
        AttrDef def = read((Element) node);
        if (def != null && !def.isBare() && !definitions.containsKey(def.name())) {
          definitions.put(def.name(), def);
        }
      } else if ("declare-styleable".equals(node.getNodeName())) {
        collect((Element) node, definitions);
      }
    }
  }

  private static AttrDef read(Element element) {
    String name = element.getAttribute("name");
    if (name.startsWith("android:")) {
      name = name.substring("android:".length());
    }
    if (name.isEmpty()) {
      return null;
    }
    List<String> formats = new ArrayList<>();
    String format = element.getAttribute("format");
    if (!format.isEmpty()) {
      for (String part : format.split("\\|")) {
        if (!part.trim().isEmpty()) {
          formats.add(part.trim());
        }
      }
    }
    List<String> values = new ArrayList<>();
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element
          && ("enum".equals(child.getNodeName()) || "flag".equals(child.getNodeName()))) {
        values.add(((Element) child).getAttribute("name"));
        if (!formats.contains(child.getNodeName())) {
          formats.add(child.getNodeName());
        }
      }
    }
    return new AttrDef(name, formats, values);
  }

  /** Whether the file declared a styleable of this name. */
  public boolean hasStyleable(String name) {
    return styleables.containsKey(name);
  }

  /**
   * The attributes of the named styleables, in the order given, each attribute once. Names that the
   * catalog does not know are skipped.
   */
  public List<AttrDef> attributes(List<String> styleableNames) {
    Map<String, AttrDef> unique = new LinkedHashMap<>();
    for (String styleable : styleableNames) {
      List<AttrDef> list = styleables.get(styleable);
      if (list == null) {
        continue;
      }
      for (AttrDef def : list) {
        if (!unique.containsKey(def.name())) {
          unique.put(def.name(), def);
        }
      }
    }
    return Collections.unmodifiableList(new ArrayList<>(unique.values()));
  }
}
