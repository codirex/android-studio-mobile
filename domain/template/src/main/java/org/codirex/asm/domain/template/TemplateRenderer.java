package org.codirex.asm.domain.template;

import java.util.Map;
import org.codirex.asm.domain.template.TemplateException.Reason;

/**
 * Replaces placeholders in text. {@code ${name}} is replaced by the value of the variable; {@code
 * $$} writes one dollar sign; every other dollar sign is kept as it is, so shell and Gradle text
 * does not need escaping.
 */
public final class TemplateRenderer {

  private TemplateRenderer() {}

  /**
   * Renders {@code text} with {@code variables}.
   *
   * @throws TemplateException with reason {@code UNKNOWN_VARIABLE} if a placeholder names a variable
   *     that is not in {@code variables}, or {@code MALFORMED} if a {@code ${} has no closing brace
   */
  public static String render(String text, Map<String, String> variables)
      throws TemplateException {
    StringBuilder out = new StringBuilder(text.length());
    int length = text.length();
    int i = 0;
    while (i < length) {
      char c = text.charAt(i);
      if (c != '$' || i + 1 >= length) {
        out.append(c);
        i++;
        continue;
      }
      char next = text.charAt(i + 1);
      if (next == '$') {
        out.append('$');
        i += 2;
      } else if (next == '{') {
        int end = text.indexOf('}', i + 2);
        if (end < 0) {
          throw new TemplateException(Reason.MALFORMED, "placeholder is not closed: " + excerpt(text, i));
        }
        String name = text.substring(i + 2, end);
        String value = variables.get(name);
        if (value == null) {
          throw new TemplateException(Reason.UNKNOWN_VARIABLE, "unknown variable: " + name);
        }
        out.append(value);
        i = end + 1;
      } else {
        out.append(c);
        i++;
      }
    }
    return out.toString();
  }

  private static String excerpt(String text, int start) {
    return text.substring(start, Math.min(text.length(), start + 20));
  }
}
