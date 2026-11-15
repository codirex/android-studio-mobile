package org.codirex.asm.domain.editor.highlight;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gradle build scripts, both the Groovy ({@code build.gradle}) and the Kotlin
 * ({@code build.gradle.kts}) flavour: comments, strings, numbers, keywords of both languages, and
 * the names of the common Gradle blocks and configurations.
 */
public final class GradleLexer implements Lexer {

  private static final Set<String> KEYWORDS =
      new HashSet<>(
          Arrays.asList(
              "as", "break", "case", "catch", "class", "continue", "def", "default", "do", "else",
              "enum", "extends", "false", "final", "finally", "for", "fun", "if", "implements",
              "import", "in", "instanceof", "interface", "is", "new", "null", "object", "package",
              "private", "public", "return", "static", "super", "switch", "this", "throw", "true",
              "try", "val", "var", "when", "while"));

  private static final Set<String> BUILTINS =
      new HashSet<>(
          Arrays.asList(
              "android", "annotationProcessor", "api", "apply", "buildscript", "buildTypes",
              "compileOnly", "defaultConfig", "dependencies", "dependencyResolutionManagement",
              "implementation", "include", "kapt", "ksp", "pluginManagement", "plugins",
              "repositories", "rootProject", "runtimeOnly", "settings", "subprojects",
              "allprojects", "tasks", "testImplementation", "androidTestImplementation"));

  @Override
  public List<Token> tokenize(CharSequence text) {
    List<Token> out = new ArrayList<>();
    int n = text.length();
    int i = 0;
    while (i < n) {
      char c = text.charAt(i);
      if (Scan.startsWith(text, i, "//")) {
        int end = Scan.lineEnd(text, i);
        out.add(new Token(i, end, TokenKind.COMMENT));
        i = end;
      } else if (Scan.startsWith(text, i, "/*")) {
        int close = Scan.indexOf(text, "*/", i + 2);
        int end = close < 0 ? n : close + 2;
        out.add(new Token(i, end, TokenKind.COMMENT));
        i = end;
      } else if (Scan.startsWith(text, i, "\"\"\"") || Scan.startsWith(text, i, "'''")) {
        String triple = text.subSequence(i, i + 3).toString();
        int close = Scan.indexOf(text, triple, i + 3);
        int end = close < 0 ? n : close + 3;
        out.add(new Token(i, end, TokenKind.STRING));
        i = end;
      } else if (c == '"' || c == '\'') {
        int end = Scan.stringEnd(text, i);
        out.add(new Token(i, end, TokenKind.STRING));
        i = end;
      } else if (c >= '0' && c <= '9') {
        int end = i + 1;
        while (end < n && isNumberChar(text.charAt(end))) {
          end++;
        }
        out.add(new Token(i, end, TokenKind.NUMBER));
        i = end;
      } else if (Character.isLetter(c) || c == '_' || c == '$') {
        int end = i + 1;
        while (end < n && isWordChar(text.charAt(end))) {
          end++;
        }
        String word = text.subSequence(i, end).toString();
        if (KEYWORDS.contains(word)) {
          out.add(new Token(i, end, TokenKind.KEYWORD));
        } else if (BUILTINS.contains(word)) {
          out.add(new Token(i, end, TokenKind.BUILTIN));
        }
        i = end;
      } else {
        i++;
      }
    }
    return out;
  }

  private static boolean isWordChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '$';
  }

  private static boolean isNumberChar(char c) {
    return (c >= '0' && c <= '9') || c == '.' || c == '_' || c == 'L' || c == 'l' || c == 'f' || c == 'F';
  }
}
