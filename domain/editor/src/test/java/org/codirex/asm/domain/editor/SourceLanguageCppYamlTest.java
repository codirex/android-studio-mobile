package org.codirex.asm.domain.editor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class SourceLanguageCppYamlTest {

  @Test
  public void yamlFilesAreYaml() {
    assertEquals(SourceLanguage.YAML, SourceLanguage.forPath("config.yaml"));
    assertEquals(SourceLanguage.YAML, SourceLanguage.forPath(".github/workflows/ci.yml"));
    assertEquals(SourceLanguage.YAML, SourceLanguage.forPath("Values.YML"));
  }

  @Test
  public void cSourcesAndHeadersAreC() {
    assertEquals(SourceLanguage.C, SourceLanguage.forPath("main.c"));
    assertEquals(SourceLanguage.C, SourceLanguage.forPath("jni/include/pty.h"));
  }

  @Test
  public void cppSourcesAndHeadersAreCpp() {
    assertEquals(SourceLanguage.CPP, SourceLanguage.forPath("main.cpp"));
    assertEquals(SourceLanguage.CPP, SourceLanguage.forPath("a.cc"));
    assertEquals(SourceLanguage.CPP, SourceLanguage.forPath("a.cxx"));
    assertEquals(SourceLanguage.CPP, SourceLanguage.forPath("a.hpp"));
    assertEquals(SourceLanguage.CPP, SourceLanguage.forPath("a.hh"));
    assertEquals(SourceLanguage.CPP, SourceLanguage.forPath("a.hxx"));
  }

  @Test
  public void nearMissesStayPlain() {
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath("a.cpp.bak"));
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath(".yaml"));
    assertEquals(SourceLanguage.PLAIN, SourceLanguage.forPath("Dockerfile"));
  }
}
