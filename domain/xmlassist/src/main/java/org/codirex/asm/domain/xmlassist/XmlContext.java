package org.codirex.asm.domain.xmlassist;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Where the caret is in an XML text, as far as completion needs to know: in a tag name, in the
 * attribute names of a tag, in an attribute value, or elsewhere. It reads the text up to the caret
 * and does not need the rest to be well formed. Immutable.
 */
public final class XmlContext {

  /** What the caret is in. */
  public enum Kind {
    /** Text, a comment, an end tag... nothing to complete. */
    NONE,
    /** The name right after {@code <}. */
    TAG_NAME,
    /** Where an attribute name is, or may be started. */
    ATTRIBUTE_NAME,
    /** The value of an attribute, with or without its opening quote. */
    ATTRIBUTE_VALUE
  }

  private final Kind kind;
  private final String tag;
  private final String parent;
  private final String attribute;
  private final String prefix;
  private final boolean quoted;
  private final Set<String> existing;

  private XmlContext(
      Kind kind,
      String tag,
      String parent,
      String attribute,
      String prefix,
      boolean quoted,
      Set<String> existing) {
    this.kind = kind;
    this.tag = tag;
    this.parent = parent;
    this.attribute = attribute;
    this.prefix = prefix;
    this.quoted = quoted;
    this.existing = Collections.unmodifiableSet(existing);
  }

  public Kind kind() {
    return kind;
  }

  /** The tag the caret is in (for a tag name: what is typed so far); null for NONE. */
  public String tag() {
    return tag;
  }

  /** The element that contains the tag; null for the root element. */
  public String parent() {
    return parent;
  }

  /** The attribute whose value the caret is in; null otherwise. */
  public String attribute() {
    return attribute;
  }

  /** What is typed of the name or the value, up to the caret. */
  public String prefix() {
    return prefix;
  }

  /** For a value: whether the opening quote is there. */
  public boolean quoted() {
    return quoted;
  }

  /** The attributes the tag has before the caret. */
  public Set<String> existing() {
    return existing;
  }

  private static XmlContext none() {
    return new XmlContext(Kind.NONE, null, null, null, "", false, new LinkedHashSet<String>());
  }

  /** The context at {@code offset} (a character index) of {@code text}. */
  public static XmlContext at(String text, int offset) {
    int limit = Math.max(0, Math.min(offset, text.length()));
    Deque<String> stack = new ArrayDeque<>();
    int i = 0;
    while (i < limit) {
      if (text.startsWith("<!--", i)) {
        int end = text.indexOf("-->", i + 4);
        if (end < 0 || end + 3 > limit) {
          return none();
        }
        i = end + 3;
      } else if (text.startsWith("<![CDATA[", i)) {
        int end = text.indexOf("]]>", i + 9);
        if (end < 0 || end + 3 > limit) {
          return none();
        }
        i = end + 3;
      } else if (text.startsWith("<?", i)) {
        int end = text.indexOf("?>", i + 2);
        if (end < 0 || end + 2 > limit) {
          return none();
        }
        i = end + 2;
      } else if (text.startsWith("<!", i)) {
        int end = text.indexOf('>', i + 2);
        if (end < 0 || end + 1 > limit) {
          return none();
        }
        i = end + 1;
      } else if (text.startsWith("</", i)) {
        int end = text.indexOf('>', i + 2);
        if (end < 0 || end + 1 > limit) {
          return none();
        }
        if (!stack.isEmpty()) {
          stack.pop();
        }
        i = end + 1;
      } else if (text.charAt(i) == '<') {
        TagScan scan = scanTag(text, i + 1, limit, stack.peek());
        if (scan.context != null) {
          return scan.context;
        }
        if (!scan.selfClosing) {
          stack.push(scan.tag);
        }
        i = scan.next;
      } else {
        i++;
      }
    }
    return none();
  }

  /** The result of reading one start tag: either the answer, or where the tag ended. */
  private static final class TagScan {
    XmlContext context;
    String tag;
    boolean selfClosing;
    int next;
  }

  private static boolean isSpace(char c) {
    return c == ' ' || c == '\n' || c == '\r' || c == '\t';
  }

  private static TagScan answer(XmlContext context) {
    TagScan scan = new TagScan();
    scan.context = context;
    return scan;
  }

  private static TagScan scanTag(String text, int start, int limit, String parent) {
    int nameEnd = start;
    while (nameEnd < text.length()) {
      char c = text.charAt(nameEnd);
      if (isSpace(c) || c == '>' || c == '/') {
        break;
      }
      nameEnd++;
    }
    String tag = text.substring(start, nameEnd);
    if (nameEnd >= limit) {
      return answer(
          new XmlContext(
              Kind.TAG_NAME,
              text.substring(start, limit),
              parent,
              null,
              text.substring(start, limit),
              false,
              new LinkedHashSet<String>()));
    }
    Set<String> existing = new LinkedHashSet<>();
    int i = nameEnd;
    while (true) {
      while (i < limit && isSpace(text.charAt(i))) {
        i++;
      }
      if (i >= limit) {
        return answer(new XmlContext(Kind.ATTRIBUTE_NAME, tag, parent, null, "", false, existing));
      }
      char c = text.charAt(i);
      if (c == '>') {
        TagScan done = new TagScan();
        done.tag = tag;
        done.next = i + 1;
        return done;
      }
      if (c == '/') {
        if (i + 1 < text.length() && text.charAt(i + 1) == '>') {
          if (i + 2 > limit) {
            return answer(none());
          }
          TagScan done = new TagScan();
          done.tag = tag;
          done.selfClosing = true;
          done.next = i + 2;
          return done;
        }
        i++;
        continue;
      }
      int nameStart = i;
      int j = i;
      while (j < text.length()) {
        char d = text.charAt(j);
        if (isSpace(d) || d == '=' || d == '>' || d == '/') {
          break;
        }
        j++;
      }
      if (j >= limit) {
        return answer(
            new XmlContext(
                Kind.ATTRIBUTE_NAME,
                tag,
                parent,
                null,
                text.substring(nameStart, limit),
                false,
                existing));
      }
      String attribute = text.substring(nameStart, j);
      existing.add(attribute);
      i = j;
      while (i < limit && isSpace(text.charAt(i))) {
        i++;
      }
      if (i >= limit) {
        return answer(new XmlContext(Kind.ATTRIBUTE_NAME, tag, parent, null, "", false, existing));
      }
      if (text.charAt(i) != '=') {
        continue;
      }
      i++;
      while (i < limit && isSpace(text.charAt(i))) {
        i++;
      }
      if (i >= limit) {
        return answer(
            new XmlContext(Kind.ATTRIBUTE_VALUE, tag, parent, attribute, "", false, existing));
      }
      char quote = text.charAt(i);
      if (quote == '"' || quote == '\'') {
        int close = text.indexOf(quote, i + 1);
        if (close < 0 || close >= limit) {
          return answer(
              new XmlContext(
                  Kind.ATTRIBUTE_VALUE,
                  tag,
                  parent,
                  attribute,
                  text.substring(i + 1, limit),
                  true,
                  existing));
        }
        i = close + 1;
      } else {
        i++;
      }
    }
  }
}
