package org.codirex.asm.domain.xmlassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class AttrCatalogTest {

  static final String ATTRS =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
          + "<resources>\n"
          + "  <!-- A comment. -->\n"
          + "  <attr name=\"textSize\" format=\"dimension\" />\n"
          + "  <attr name=\"textStyle\"><flag name=\"normal\" value=\"0\"/><flag name=\"bold\" value=\"1\"/></attr>\n"
          + "  <declare-styleable name=\"TextView\"><attr name=\"textSize\"/><attr name=\"textStyle\"/></declare-styleable>\n"
          + "  <declare-styleable name=\"View\">\n"
          + "    <attr name=\"id\" format=\"reference\"/>\n"
          + "    <attr name=\"clickable\" format=\"boolean\"/>\n"
          + "    <attr name=\"visibility\"><enum name=\"visible\" value=\"0\"/><enum name=\"gone\" value=\"2\"/></attr>\n"
          + "  </declare-styleable>\n"
          + "  <declare-styleable name=\"ViewGroup_Layout\">\n"
          + "    <attr name=\"layout_width\" format=\"dimension\"><enum name=\"match_parent\" value=\"-1\"/><enum name=\"wrap_content\" value=\"-2\"/></attr>\n"
          + "  </declare-styleable>\n"
          + "  <declare-styleable name=\"LinearLayout_Layout\"><attr name=\"layout_weight\" format=\"float\"/></declare-styleable>\n"
          + "</resources>\n";

  static AttrCatalog catalog() throws IOException {
    return AttrCatalog.parse(new ByteArrayInputStream(ATTRS.getBytes(StandardCharsets.UTF_8)));
  }

  private static List<String> names(List<AttrDef> defs) {
    List<String> names = new ArrayList<>();
    for (AttrDef def : defs) {
      names.add(def.name());
    }
    return names;
  }

  @Test
  public void attributesComeInTheOrderOfTheStyleablesAndAreResolved() throws IOException {
    AttrCatalog catalog = catalog();
    List<AttrDef> defs = catalog.attributes(Arrays.asList("TextView", "View", "Nope"));
    assertEquals(
        Arrays.asList("textSize", "textStyle", "id", "clickable", "visibility"), names(defs));
    assertEquals(Arrays.asList("dimension"), defs.get(0).formats());
    assertEquals(Arrays.asList("normal", "bold"), defs.get(1).values());
    assertTrue(defs.get(1).formats().contains("flag"));
    assertEquals(Arrays.asList("visible", "gone"), defs.get(4).values());
  }

  @Test
  public void anAttributeIsListedOnceWhenTwoStyleablesHaveIt() throws IOException {
    List<AttrDef> defs = catalog().attributes(Arrays.asList("View", "View"));
    assertEquals(3, defs.size());
  }

  @Test
  public void knowsWhichStyleablesExist() throws IOException {
    assertTrue(catalog().hasStyleable("LinearLayout_Layout"));
    assertEquals(false, catalog().hasStyleable("Gallery"));
  }

  @Test(expected = IOException.class)
  public void somethingThatIsNotXmlIsRejected() throws IOException {
    AttrCatalog.parse(new ByteArrayInputStream("not xml".getBytes(StandardCharsets.UTF_8)));
  }
}
