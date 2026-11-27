package org.codirex.asm.domain.xmlassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProjectViewsTest {

  @Rule public TemporaryFolder temp = new TemporaryFolder();

  private static void write(File file, String text) throws IOException {
    file.getParentFile().mkdirs();
    Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
  }

  private LibraryIndex index() throws IOException {
    File src = new File(temp.getRoot(), "app/src/main/java");
    write(
        new File(src, "org/example/CircleView.java"),
        "package org.example;\npublic class CircleView extends View {\n}\n");
    write(
        new File(src, "org/example/Badge.kt"),
        "package org.example\n"
            + "class Badge @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)"
            + " : AppCompatTextView(context, attrs) {\n}\n");
    write(
        new File(src, "org/example/FancyBadge.java"),
        "package org.example;\npublic class FancyBadge extends Badge {\n}\n");
    write(new File(src, "org/example/Helper.java"), "package org.example;\npublic class Helper extends Object {\n}\n");
    File res = new File(temp.getRoot(), "app/src/main/res");
    write(
        new File(res, "values/attrs.xml"),
        "<resources>\n<attr name=\"radius\" format=\"dimension\"/>\n"
            + "<declare-styleable name=\"CircleView\"><attr name=\"radius\"/></declare-styleable>\n"
            + "<declare-styleable name=\"Badge\"><attr name=\"badgeText\" format=\"string\"/></declare-styleable>\n"
            + "</resources>\n");
    File list = new File(temp.getRoot(), "app/.asm/libs.txt");
    write(list, "res " + res.getAbsolutePath() + "\nsrc " + src.getAbsolutePath() + "\n");
    return LibraryIndex.load(list);
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
  public void viewsOfTheProjectAreFoundBySuperclass() throws IOException {
    LibraryIndex index = index();
    assertTrue(index.views().contains("org.example.CircleView"));
    assertTrue(index.views().contains("org.example.Badge"));
    assertTrue(index.views().contains("org.example.FancyBadge"));
    assertFalse(index.views().contains("org.example.Helper"));
    assertEquals("TextView", index.frameworkBase("FancyBadge"));
  }

  @Test
  public void projectViewsAreProposedAsTags() throws IOException {
    assertTrue(labels("<LinearLayout>\n<Circ").contains("CircleView"));
  }

  @Test
  public void customAttributesFollowTheViewAndItsSuperclasses() throws IOException {
    List<String> labels = labels("<LinearLayout>\n<org.example.FancyBadge ");
    assertTrue(labels.contains("app:badgeText"));
    assertTrue(labels.contains("android:textSize"));
    assertFalse(labels.contains("app:radius"));
    assertTrue(labels("<org.example.CircleView ").contains("app:radius"));
  }

  @Test
  public void toolsHasTheFrameworkAttributesAndItsOwn() throws IOException {
    List<String> labels = labels("<TextView tools:");
    assertTrue(labels.contains("tools:textSize"));
    assertTrue(labels.contains("tools:context"));
    assertTrue(labels.contains("tools:listitem"));
    assertFalse(labels.contains("android:textSize"));
    assertEquals(
        java.util.Collections.singletonList("wrap_content"), labels("<View tools:layout_width=\"w"));
  }
}
