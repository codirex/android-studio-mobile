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
  public void dataAndDocumentFiles() {
    assertEquals(SourceLanguage.XML, SourceLanguage.forPath("app/src/main/AndroidManifest.xml"));
    assertEquals(SourceLanguage.JSON, SourceLanguage.forPath("package.json"));
    assertEquals(SourceLanguage.MARKDOWN, SourceLanguage.forPath("README.md"));
    assertEquals(SourceLanguage.MARKDOWN, SourceLanguage.forPath("notes.markdown"));
  }

  @Test
  public void gradleScripts() {
    assertEquals(SourceLanguage.GRADLE, SourceLanguage.forPath("build.gradle"));
    assertEquals(SourceLanguage.GRADLE, SourceLanguage.forPath("app/build.gradle.kts"));
    assertEquals(SourceLanguage.GRADLE, SourceLanguage.forPath("settings.gradle.kts"));
  }

  @Test
  public void otherKotlinScriptsArePlain() {
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath("tool.kts"));
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
