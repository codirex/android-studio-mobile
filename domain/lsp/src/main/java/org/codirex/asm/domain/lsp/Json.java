package org.codirex.asm.domain.lsp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JSON reader and writer for the language server protocol. Values are {@link Map} (objects,
 * in document order), {@link List}, {@link String}, {@link Long} (a number without fraction or
 * exponent that fits), {@link Double}, {@link Boolean} and {@code null}.
 *
 * <p>The protocol needs nothing more, and the domain layer has no library to lean on (ADR-0003).
 */
public final class Json {

  /** Deeper documents are refused instead of overflowing the stack. */
  private static final int MAX_DEPTH = 200;

  private Json() {}

  /** Reads one JSON value; anything after it other than white space is an error. */
  public static Object parse(String text) throws JsonException {
    Parser parser = new Parser(text);
    parser.skipSpace();
    Object value = parser.value(0);
    parser.skipSpace();
    if (!parser.atEnd()) {
      throw parser.error("unexpected text after the value");
    }
    return value;
  }

  /** Writes a value built from the types above. */
  public static String write(Object value) throws JsonException {
    StringBuilder out = new StringBuilder();
    write(value, out, 0);
    return out.toString();
  }

  private static void write(Object value, StringBuilder out, int depth) throws JsonException {
    if (depth > MAX_DEPTH) {
      throw new JsonException("value nested too deeply");
    }
    if (value == null) {
      out.append("null");
    } else if (value instanceof String) {
      quote((String) value, out);
    } else if (value instanceof Boolean) {
      out.append(((Boolean) value).booleanValue() ? "true" : "false");
    } else if (value instanceof Double || value instanceof Float) {
      double number = ((Number) value).doubleValue();
      if (Double.isNaN(number) || Double.isInfinite(number)) {
        throw new JsonException("not a JSON number: " + number);
      }
      out.append(number);
    } else if (value instanceof Number) {
      out.append(((Number) value).longValue());
    } else if (value instanceof Map) {
      out.append('{');
      boolean first = true;
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        if (!(entry.getKey() instanceof String)) {
          throw new JsonException("object keys must be text");
        }
        if (!first) {
          out.append(',');
        }
        first = false;
        quote((String) entry.getKey(), out);
        out.append(':');
        write(entry.getValue(), out, depth + 1);
      }
      out.append('}');
    } else if (value instanceof Iterable) {
      out.append('[');
      boolean first = true;
      for (Object element : (Iterable<?>) value) {
        if (!first) {
          out.append(',');
        }
        first = false;
        write(element, out, depth + 1);
      }
      out.append(']');
    } else {
      throw new JsonException("cannot write " + value.getClass().getName());
    }
  }

  private static void quote(String text, StringBuilder out) {
    out.append('"');
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"':
          out.append("\\\"");
          break;
        case '\\':
          out.append("\\\\");
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\t':
          out.append("\\t");
          break;
        case '\b':
          out.append("\\b");
          break;
        case '\f':
          out.append("\\f");
          break;
        default:
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
      }
    }
    out.append('"');
  }

  private static final class Parser {

    private final String text;
    private int position;

    Parser(String text) {
      this.text = text;
    }

    boolean atEnd() {
      return position >= text.length();
    }

    JsonException error(String message) {
      return new JsonException(message + " at " + position);
    }

    void skipSpace() {
      while (position < text.length()) {
        char c = text.charAt(position);
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
          position++;
        } else {
          break;
        }
      }
    }

    Object value(int depth) throws JsonException {
      if (depth > MAX_DEPTH) {
        throw error("value nested too deeply");
      }
      if (atEnd()) {
        throw error("unexpected end");
      }
      char c = text.charAt(position);
      switch (c) {
        case '{':
          return object(depth);
        case '[':
          return array(depth);
        case '"':
          return string();
        case 't':
          return literal("true", Boolean.TRUE);
        case 'f':
          return literal("false", Boolean.FALSE);
        case 'n':
          return literal("null", null);
        default:
          if (c == '-' || (c >= '0' && c <= '9')) {
            return number();
          }
          throw error("unexpected character '" + c + "'");
      }
    }

    private Object literal(String word, Object value) throws JsonException {
      if (!text.startsWith(word, position)) {
        throw error("expected " + word);
      }
      position += word.length();
      return value;
    }

    private Map<String, Object> object(int depth) throws JsonException {
      Map<String, Object> result = new LinkedHashMap<>();
      position++;
      skipSpace();
      if (peek() == '}') {
        position++;
        return result;
      }
      while (true) {
        skipSpace();
        if (peek() != '"') {
          throw error("expected a key");
        }
        String key = string();
        skipSpace();
        if (peek() != ':') {
          throw error("expected ':'");
        }
        position++;
        skipSpace();
        result.put(key, value(depth + 1));
        skipSpace();
        char next = peek();
        position++;
        if (next == '}') {
          return result;
        }
        if (next != ',') {
          throw error("expected ',' or '}'");
        }
      }
    }

    private List<Object> array(int depth) throws JsonException {
      List<Object> result = new ArrayList<>();
      position++;
      skipSpace();
      if (peek() == ']') {
        position++;
        return result;
      }
      while (true) {
        skipSpace();
        result.add(value(depth + 1));
        skipSpace();
        char next = peek();
        position++;
        if (next == ']') {
          return result;
        }
        if (next != ',') {
          throw error("expected ',' or ']'");
        }
      }
    }

    private char peek() throws JsonException {
      if (atEnd()) {
        throw error("unexpected end");
      }
      return text.charAt(position);
    }

    private String string() throws JsonException {
      StringBuilder out = new StringBuilder();
      position++;
      while (true) {
        if (atEnd()) {
          throw error("unterminated string");
        }
        char c = text.charAt(position++);
        if (c == '"') {
          return out.toString();
        }
        if (c < 0x20) {
          throw error("control character in a string");
        }
        if (c != '\\') {
          out.append(c);
          continue;
        }
        if (atEnd()) {
          throw error("unterminated string");
        }
        char escape = text.charAt(position++);
        switch (escape) {
          case '"':
          case '\\':
          case '/':
            out.append(escape);
            break;
          case 'n':
            out.append('\n');
            break;
          case 'r':
            out.append('\r');
            break;
          case 't':
            out.append('\t');
            break;
          case 'b':
            out.append('\b');
            break;
          case 'f':
            out.append('\f');
            break;
          case 'u':
            if (position + 4 > text.length()) {
              throw error("short unicode escape");
            }
            try {
              out.append((char) Integer.parseInt(text.substring(position, position + 4), 16));
            } catch (NumberFormatException e) {
              throw error("bad unicode escape");
            }
            position += 4;
            break;
          default:
            throw error("bad escape '\\" + escape + "'");
        }
      }
    }

    private Object number() throws JsonException {
      int start = position;
      if (peek() == '-') {
        position++;
      }
      int digits = 0;
      while (!atEnd() && Character.isDigit(text.charAt(position))) {
        position++;
        digits++;
      }
      if (digits == 0) {
        throw error("bad number");
      }
      boolean integral = true;
      if (!atEnd() && text.charAt(position) == '.') {
        integral = false;
        position++;
        int fraction = 0;
        while (!atEnd() && Character.isDigit(text.charAt(position))) {
          position++;
          fraction++;
        }
        if (fraction == 0) {
          throw error("bad number");
        }
      }
      if (!atEnd() && (text.charAt(position) == 'e' || text.charAt(position) == 'E')) {
        integral = false;
        position++;
        if (!atEnd() && (text.charAt(position) == '+' || text.charAt(position) == '-')) {
          position++;
        }
        int exponent = 0;
        while (!atEnd() && Character.isDigit(text.charAt(position))) {
          position++;
          exponent++;
        }
        if (exponent == 0) {
          throw error("bad number");
        }
      }
      String token = text.substring(start, position);
      try {
        if (integral) {
          try {
            return Long.valueOf(token);
          } catch (NumberFormatException tooBig) {
            return Double.valueOf(token);
          }
        }
        return Double.valueOf(token);
      } catch (NumberFormatException e) {
        throw error("bad number");
      }
    }
  }
}
