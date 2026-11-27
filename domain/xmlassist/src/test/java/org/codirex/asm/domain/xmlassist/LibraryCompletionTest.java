package org.codirex.asm.domain.xmlassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LibraryCompletionTest {

  @Rule public TemporaryFolder temp = new TemporaryFolder();

  private static final String VALUES =
      "<resources>\n"
          + "  <attr name=\"layout_constraintTop_toTopOf\" format=\"reference|enum\">\n"
          + "    <enum name=\"parent\" value=\"0\"/>\n"
          + "  </attr>\n"
          + "  <attr name=\"cornerRadius\" format=\"dimension\"/>\n"
          + "  <declare-styleable name=\"ConstraintLayout_Layout\">\n"
          + "    <attr name=\"layout_constraintTop_toTopOf\"/>\n"
          + "    <attr name=\"android:orientation\"/>\n"
          + "  </declare-styleable>\n"
          + "  <declare-styleable name=\"MaterialButton\">\n"
          + "    <attr name=\"cornerRadius\"/>\n"
          + "    <attr name=\"android:insetLeft\"/>\n"
          + "  </declare-styleable>\n"
          + "  <declare-styleable name=\"AppCompatTextView\">\n"
          + "    <attr name=\"autoSizeTextType\" format=\"enum\"><enum name=\"none\" value=\"0\"/><enum name=\"uniform\" value=\"1\"/></attr>\n"
          + "  </declare-styleable>\n"
          + "</resources>\n";

  private LibraryIndex cached;

  private LibraryIndex index() throws IOException {
    if (cached == null) {
      cached = build();
    }
    return cached;
  }

  private LibraryIndex build() throws IOException {
    File module = temp.newFolder("app");
    File res = new File(temp.getRoot(), "aar/res");
    new File(res, "values").mkdirs();
    Files.write(new File(res, "values/values.xml").toPath(), VALUES.getBytes(StandardCharsets.UTF_8));
    File jar = temp.newFile("lib.jar");
    try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jar))) {
      for (String entry :
          new String[] {
            "androidx/constraintlayout/widget/ConstraintLayout.class",
            "com/google/android/material/button/MaterialButton.class",
            "com/google/android/material/button/MaterialButton$Inner.class",
            "com/google/android/material/other/Unrelated.class"
          }) {
        zip.putNextEntry(new ZipEntry(entry));
        zip.closeEntry();
      }
    }
    File asm = new File(module, ".asm");
    asm.mkdirs();
    Files.write(
        new File(asm, "libs.txt").toPath(),
        ("res " + res.getAbsolutePath() + "\njar " + jar.getAbsolutePath() + "\n")
            .getBytes(StandardCharsets.UTF_8));
    assertNotNull(LibraryIndex.listOf(module));
    return LibraryIndex.load(LibraryIndex.listOf(module));
  }

  private List<String> labels(String text) throws IOException {
    List<String> out = new ArrayList<>();
    for (AndroidXmlCompleter.Proposal p :
        new AndroidXmlCompleter(AttrCatalogTest.catalog(), ResourceIndex.empty(), index())
            .complete(text, text.length())) {
      out.add(p.label());
    }
    return out;
  }

  @Test
  public void notSyncedMeansNoList() throws IOException {
    assertNull(LibraryIndex.listOf(temp.newFolder("plain")));
  }

  @Test
  public void viewsComeFromTheJarsByStyleableName() throws IOException {
    LibraryIndex index = index();
    assertTrue(index.views().contains("androidx.constraintlayout.widget.ConstraintLayout"));
    assertTrue(index.views().contains("com.google.android.material.button.MaterialButton"));
    assertEquals(2, index.views().size());
  }

  @Test
  public void libraryTagsInsertTheFullName() throws IOException {
    AndroidXmlCompleter completer =
        new AndroidXmlCompleter(AttrCatalogTest.catalog(), ResourceIndex.empty(), index());
    String text = "<Constr";
    List<AndroidXmlCompleter.Proposal> list = completer.complete(text, text.length());
    assertEquals(1, list.size());
    assertEquals("ConstraintLayout", list.get(0).label());
    assertEquals("androidx.constraintlayout.widget.ConstraintLayout", list.get(0).insertText());
    assertEquals(6, list.get(0).replaceLength());
    assertEquals(1, completer.complete("<androidx.con", 13).size());
  }

  @Test
  public void appAttributesOfAChildFollowTheParentLibrary() throws IOException {
    List<String> labels = labels("<androidx.constraintlayout.widget.ConstraintLayout>\n<Button app:");
    assertTrue(labels.contains("app:layout_constraintTop_toTopOf"));
    assertTrue(labels.contains("app:cornerRadius"));
    assertTrue(labels.contains("app:autoSizeTextType"));
    assertFalse(labels.contains("app:orientation"));
    assertFalse(labels.contains("app:insetLeft"));
    assertFalse(labels.contains("android:layout_width"));
  }

  @Test
  public void withoutNamespaceBothAreProposed() throws IOException {
    List<String> labels = labels("<LinearLayout>\n<Button corner");
    assertTrue(labels.contains("app:cornerRadius"));
    labels = labels("<Button text");
    assertTrue(labels.contains("android:textSize"));
  }

  @Test
  public void appValuesUseTheLibraryDefinition() throws IOException {
    List<String> labels = labels("<androidx.constraintlayout.widget.ConstraintLayout>\n<Button app:layout_constraintTop_toTopOf=\"");
    assertTrue(labels.contains("parent"));
    labels = labels("<TextView app:autoSizeTextType=\"u");
    assertEquals(java.util.Collections.singletonList("uniform"), labels);
  }

  @Test
  public void withoutLibrariesAppAttributesAreEmpty() throws IOException {
    AndroidXmlCompleter completer = new AndroidXmlCompleter(AttrCatalogTest.catalog());
    String text = "<Button app:";
    assertTrue(completer.complete(text, text.length()).isEmpty());
  }
}
