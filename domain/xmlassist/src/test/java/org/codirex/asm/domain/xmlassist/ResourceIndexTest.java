package org.codirex.asm.domain.xmlassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ResourceIndexTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private void write(String path, String text) throws IOException {
    File file = new File(folder.getRoot(), path);
    assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(text.getBytes(StandardCharsets.UTF_8));
    }
  }

  @Test
  public void readsValuesFilesAndIds() throws IOException {
    write(
        "app/src/main/res/values/strings.xml",
        "<resources><string name=\"app_name\">A</string><string name=\"hello\">H</string>"
            + "<color name=\"brand\">#fff</color><style name=\"Theme.App\"/>"
            + "<string-array name=\"planets\"/><item name=\"fab\" type=\"id\"/>"
            + "<declare-styleable name=\"X\"/></resources>");
    write("app/src/main/res/values-ar/strings.xml", "<resources><string name=\"app_name\">B</string></resources>");
    write("app/src/main/res/drawable/ic_a.xml", "<vector/>");
    write("app/src/main/res/drawable-hdpi/logo.9.png", "x");
    write(
        "app/src/main/res/layout/main.xml",
        "<LinearLayout android:id=\"@+id/root\"><Button android:id=\"@+id/btn\" android:labelFor=\"@id/root\"/></LinearLayout>");

    ResourceIndex index = ResourceIndex.scan(folder.getRoot());

    assertEquals(Arrays.asList("app_name", "hello"), Arrays.asList(index.names("string").toArray()));
    assertEquals(Arrays.asList("brand"), Arrays.asList(index.names("color").toArray()));
    assertEquals(Arrays.asList("Theme.App"), Arrays.asList(index.names("style").toArray()));
    assertEquals(Arrays.asList("planets"), Arrays.asList(index.names("array").toArray()));
    assertEquals(Arrays.asList("btn", "fab", "root"), Arrays.asList(index.names("id").toArray()));
    assertEquals(Arrays.asList("ic_a", "logo"), Arrays.asList(index.names("drawable").toArray()));
    assertEquals(Arrays.asList("main"), Arrays.asList(index.names("layout").toArray()));
    assertTrue(index.names("nothing").isEmpty());
    assertTrue(index.types().contains("string"));
  }

  @Test
  public void onlyMainResourceFoldersCountAndBuildOutputIsSkipped() throws IOException {
    write("app/build.gradle", "");
    write("app/build/intermediates/src/main/res/values/x.xml", "<resources><string name=\"generated\">g</string></resources>");
    write("app/src/test/res/values/y.xml", "<resources><string name=\"test_only\">t</string></resources>");
    write("feature/build/src/main/res/values/z.xml", "<resources><string name=\"kept\">k</string></resources>");

    ResourceIndex index = ResourceIndex.scan(folder.getRoot());

    assertEquals(Arrays.asList("kept"), Arrays.asList(index.names("string").toArray()));
  }

  @Test
  public void aBrokenValuesFileIsIgnored() throws IOException {
    write("app/src/main/res/values/bad.xml", "<resources><string name=\"a\"");
    write("app/src/main/res/values/good.xml", "<resources><string name=\"b\">B</string></resources>");
    assertEquals(
        Arrays.asList("b"), Arrays.asList(ResourceIndex.scan(folder.getRoot()).names("string").toArray()));
  }

  @Test
  public void anEmptyProjectHasNothing() {
    assertTrue(ResourceIndex.scan(folder.getRoot()).types().isEmpty());
    assertTrue(ResourceIndex.empty().types().isEmpty());
  }
}
