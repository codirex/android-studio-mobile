package org.codirex.asm.domain.xmlassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class AndroidXmlCompleterTest {

  private static List<String> labels(String text) throws IOException {
    AndroidXmlCompleter completer = new AndroidXmlCompleter(AttrCatalogTest.catalog());
    List<String> labels = new ArrayList<>();
    for (AndroidXmlCompleter.Proposal p : completer.complete(text, text.length())) {
      labels.add(p.label());
    }
    return labels;
  }

  private static List<AndroidXmlCompleter.Proposal> proposals(String text) throws IOException {
    return new AndroidXmlCompleter(AttrCatalogTest.catalog()).complete(text, text.length());
  }

  @Test
  public void attributesOfTheClassTheSuperclassesAndTheParentLayout() throws IOException {
    List<String> labels = labels("<LinearLayout>\n<TextView ");
    assertTrue(labels.contains("android:textSize"));
    assertTrue(labels.contains("android:id"));
    assertTrue(labels.contains("android:layout_weight"));
    assertTrue(labels.contains("android:layout_width"));
  }

  @Test
  public void layoutParametersFollowTheParent() throws IOException {
    assertFalse(labels("<FrameLayout>\n<TextView ").contains("android:layout_weight"));
    assertFalse(labels("<TextView ").contains("android:layout_weight"));
  }

  @Test
  public void anAttributeThatIsThereAlreadyIsNotProposedAgain() throws IOException {
    assertFalse(labels("<TextView android:textSize=\"1sp\" ").contains("android:textSize"));
  }

  @Test
  public void anAttributeNameInsertsTheEqualsSignAndQuotes() throws IOException {
    List<AndroidXmlCompleter.Proposal> found = proposals("<TextView android:textS");
    assertEquals(2, found.size());
    AndroidXmlCompleter.Proposal first = found.get(0);
    assertEquals("android:textSize", first.label());
    assertEquals("android:textSize=\"\"", first.insertText());
    assertEquals("android:textS".length(), first.replaceLength());
    assertEquals("dimension", first.detail());
  }

  @Test
  public void otherNamespacesGetNothingHere() throws IOException {
    assertTrue(proposals("<TextView app:te").isEmpty());
  }

  @Test
  public void flagValuesAreProposedAfterABar() throws IOException {
    List<AndroidXmlCompleter.Proposal> found = proposals("<TextView android:textStyle=\"bold|n");
    assertEquals(1, found.size());
    assertEquals("normal", found.get(0).insertText());
    assertEquals(1, found.get(0).replaceLength());
  }

  @Test
  public void valuesInsideQuotesAreInsertedWithoutQuotes() throws IOException {
    List<AndroidXmlCompleter.Proposal> found = proposals("<TextView android:textStyle=\"");
    assertEquals(2, found.size());
    assertEquals("normal", found.get(0).insertText());
    assertEquals(0, found.get(0).replaceLength());
  }

  @Test
  public void anUnquotedValueGetsItsQuotes() throws IOException {
    List<AndroidXmlCompleter.Proposal> found = proposals("<View android:visibility=");
    assertEquals("\"visible\"", found.get(0).insertText());
  }

  @Test
  public void booleansAndReferencesAreProposed() throws IOException {
    List<String> bools = labels("<View android:clickable=\"");
    assertEquals(java.util.Arrays.asList("true", "false"), bools);
    assertTrue(labels("<View android:id=\"@").contains("@+id/"));
  }

  @Test
  public void sizesProposeTheEnumValues() throws IOException {
    List<String> labels = labels("<View android:layout_width=\"w");
    assertEquals(java.util.Arrays.asList("wrap_content"), labels);
  }

  @Test
  public void tagsAreProposed() throws IOException {
    List<String> labels = labels("<LinearLayout>\n<Tex");
    assertEquals("TextView", labels.get(0));
    assertFalse(labels.contains("Button"));
    assertTrue(labels("<").contains("Button"));
  }

  @Test
  public void textAndCommentsGetNothing() throws IOException {
    assertTrue(labels("<A>hello").isEmpty());
    assertTrue(labels("<!-- <TextView ").isEmpty());
  }

  @Test
  public void onlyLayoutsAreCompletedHere() {
    assertTrue(AndroidXmlCompleter.appliesTo("app/src/main/res/layout/activity_main.xml"));
    assertTrue(AndroidXmlCompleter.appliesTo("app/src/main/res/layout-land/main.xml"));
    assertFalse(AndroidXmlCompleter.appliesTo("app/src/main/res/values/strings.xml"));
    assertFalse(AndroidXmlCompleter.appliesTo("app/src/main/AndroidManifest.xml"));
    assertFalse(AndroidXmlCompleter.appliesTo("app/src/main/res/layout/notes.txt"));
  }

  @Test
  public void offsetsFollowLinesAndColumns() {
    String text = "ab\ncde\nf";
    assertEquals(0, AndroidXmlCompleter.offsetOf(text, 0, 0));
    assertEquals(5, AndroidXmlCompleter.offsetOf(text, 1, 2));
    assertEquals(text.length(), AndroidXmlCompleter.offsetOf(text, 9, 0));
  }
}
