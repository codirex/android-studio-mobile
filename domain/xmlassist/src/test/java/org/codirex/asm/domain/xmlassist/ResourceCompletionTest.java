package org.codirex.asm.domain.xmlassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ResourceCompletionTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private static final String ATTRS =
      "<resources><declare-styleable name=\"TextView\">"
          + "<attr name=\"text\" format=\"string\"/><attr name=\"lines\" format=\"integer\"/>"
          + "</declare-styleable></resources>";

  private AndroidXmlCompleter completer() throws IOException {
    File values = new File(folder.getRoot(), "app/src/main/res/values");
    assertTrue(values.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(values, "s.xml"))) {
      out.write(
          ("<resources><string name=\"app_name\">A</string><string name=\"about\">B</string>"
                  + "<string name=\"hello\">C</string><color name=\"brand\">#fff</color></resources>")
              .getBytes(StandardCharsets.UTF_8));
    }
    AttrCatalog catalog =
        AttrCatalog.parse(new ByteArrayInputStream(ATTRS.getBytes(StandardCharsets.UTF_8)));
    return new AndroidXmlCompleter(catalog, ResourceIndex.scan(folder.getRoot()));
  }

  private List<String> labels(String text) throws IOException {
    List<String> labels = new ArrayList<>();
    for (AndroidXmlCompleter.Proposal p : completer().complete(text, text.length())) {
      labels.add(p.label());
    }
    return labels;
  }

  @Test
  public void namesOfTheProjectAreProposedAfterTheTypeAndSlash() throws IOException {
    List<String> labels = labels("<TextView android:text=\"@string/a");
    assertEquals(java.util.Arrays.asList("@string/about", "@string/app_name"), labels);
  }

  @Test
  public void aNameReplacesTheWholeReferenceTyped() throws IOException {
    String text = "<TextView android:text=\"@string/a";
    AndroidXmlCompleter.Proposal first = completer().complete(text, text.length()).get(0);
    assertEquals("@string/about", first.insertText());
    assertEquals("@string/a".length(), first.replaceLength());
  }

  @Test
  public void rootsIncludeTheTypesOfTheProject() throws IOException {
    List<String> labels = labels("<TextView android:text=\"@");
    assertTrue(labels.contains("@string/"));
    assertTrue(labels.contains("@color/"));
  }

  @Test
  public void anEmptyValueOffersTheRootsForAStringAttribute() throws IOException {
    assertTrue(labels("<TextView android:text=\"").contains("@string/"));
  }

  @Test
  public void anIntegerAttributeGetsNoReferences() throws IOException {
    assertTrue(labels("<TextView android:lines=\"").isEmpty());
  }

  @Test
  public void androidNamespaceReferencesAreLeftAlone() throws IOException {
    assertTrue(labels("<TextView android:text=\"@android:string/a").isEmpty());
  }
}
