package org.codirex.asm.domain.editor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class SourceLanguageTest {

  @Test
  public void javaFilesAreJava() {
    assertEquals(SourceLanguage.JAVA, SourceLanguage.forPath("Main.java"));
    assertEquals(SourceLanguage.JAVA, SourceLanguage.forPath("app/src/main/java/org/App.java"));
  }

  @Test
  public void theExtensionIsCaseInsensitive() {
    assertEquals(SourceLanguage.JAVA, SourceLanguage.forPath("Main.JAVA"));
    assertEquals(SourceLanguage.JAVA, SourceLanguage.forPath("Main.Java"));
    assertEquals(SourceLanguage.XML, SourceLanguage.forPath("Layout.XML"));
  }

  @Test
  public void androidProjectFiles() {
    assertEquals(SourceLanguage.XML, SourceLanguage.forPath("app/src/main/AndroidManifest.xml"));
    assertEquals(SourceLanguage.GROOVY, SourceLanguage.forPath("build.gradle"));
    assertEquals(SourceLanguage.KOTLIN, SourceLanguage.forPath("app/build.gradle.kts"));
    assertEquals(SourceLanguage.KOTLIN, SourceLanguage.forPath("Main.kt"));
    assertEquals(SourceLanguage.PROPERTIES, SourceLanguage.forPath("gradle.properties"));
  }

  @Test
  public void dataAndDocumentFiles() {
    assertEquals(SourceLanguage.JSON, SourceLanguage.forPath("package.json"));
    assertEquals(SourceLanguage.MARKDOWN, SourceLanguage.forPath("README.md"));
    assertEquals(SourceLanguage.MARKDOWN, SourceLanguage.forPath("notes.markdown"));
    assertEquals(SourceLanguage.HTML, SourceLanguage.forPath("index.htm"));
    assertEquals(SourceLanguage.CSS, SourceLanguage.forPath("site.css"));
  }

  @Test
  public void scriptFiles() {
    assertEquals(SourceLanguage.JAVASCRIPT, SourceLanguage.forPath("app.js"));
    assertEquals(SourceLanguage.PYTHON, SourceLanguage.forPath("tool.py"));
    assertEquals(SourceLanguage.SHELL, SourceLanguage.forPath("scripts/run.sh"));
  }

  @Test
  public void otherFilesArePlain() {
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath("notes.txt"));
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath("Main.javax"));
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath("Main.java.bak"));
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath("Makefile"));
  }

  @Test
  public void hiddenFilesAndTrailingDotsHaveNoExtension() {
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath(".java"));
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath("src/.java"));
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath("Main."));
  }

  @Test
  public void onlyTheFileNameCounts() {
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath("pkg.java/readme"));
    assertEquals(SourceLanguage.JAVA, SourceLanguage.forPath("pkg.txt/Main.java"));
  }

  @Test
  public void noPathIsPlain() {
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath(null));
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath(""));
  }
}
