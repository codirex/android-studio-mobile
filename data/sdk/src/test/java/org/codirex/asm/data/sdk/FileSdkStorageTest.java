package org.codirex.asm.data.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class FileSdkStorageTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private File zip(String... namesAndContents) throws Exception {
    File file = tmp.newFile();
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
      for (int i = 0; i < namesAndContents.length; i += 2) {
        out.putNextEntry(new ZipEntry(namesAndContents[i]));
        if (!namesAndContents[i].endsWith("/")) {
          out.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
        }
        out.closeEntry();
      }
    }
    return file;
  }

  private static String read(File file) throws IOException {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }

  @Test
  public void aPackageIsUnpackedWithoutItsTopLevelFolderAndMarked() throws Exception {
    File root = tmp.newFolder("sdk");
    FileSdkStorage storage = new FileSdkStorage(root);
    storage.install(
        "platforms;android-34",
        "3",
        zip("android-34/", "", "android-34/android.jar", "JAR", "android-34/data/res.txt", "R"));
    assertEquals("JAR", read(new File(root, "platforms/android-34/android.jar")));
    assertEquals("R", read(new File(root, "platforms/android-34/data/res.txt")));
    Map<String, String> installed = storage.installed();
    assertEquals(1, installed.size());
    assertEquals("3", installed.get("platforms;android-34"));
    assertFalse(new File(root, "platforms/android-34.installing").exists());
  }

  @Test
  public void anArchiveWithSeveralRootsIsKeptAsItIs() throws Exception {
    File root = tmp.newFolder("sdk");
    new FileSdkStorage(root).install("build-tools;34.0.0", "34.0.0", zip("lib/d8.jar", "D8", "source.properties", "P"));
    assertEquals("D8", read(new File(root, "build-tools/34.0.0/lib/d8.jar")));
    assertEquals("P", read(new File(root, "build-tools/34.0.0/source.properties")));
  }

  @Test
  public void installingAgainReplacesTheOldPackage() throws Exception {
    File root = tmp.newFolder("sdk");
    FileSdkStorage storage = new FileSdkStorage(root);
    storage.install("platforms;android-34", "2", zip("a/old.txt", "old"));
    storage.install("platforms;android-34", "3", zip("a/new.txt", "new"));
    assertFalse(new File(root, "platforms/android-34/old.txt").exists());
    assertTrue(new File(root, "platforms/android-34/new.txt").isFile());
    assertEquals("3", storage.installed().get("platforms;android-34"));
    assertFalse(new File(root, "platforms/android-34.old").exists());
  }

  @Test
  public void anEntryThatLeavesThePackageIsRefusedAndNothingStaysBehind() throws Exception {
    File root = tmp.newFolder("sdk");
    FileSdkStorage storage = new FileSdkStorage(root);
    try {
      storage.install("platforms;android-34", "3", zip("ok.txt", "x", "../evil.txt", "bad"));
      fail("installed");
    } catch (IOException expected) {
      // expected
    }
    assertTrue(storage.installed().isEmpty());
    assertFalse(new File(root, "platforms/android-34").exists());
    assertFalse(new File(root, "platforms/android-34.installing").exists());
    assertFalse(new File(root, "platforms/evil.txt").exists());
  }

  @Test
  public void aFailedUpdateKeepsTheInstalledVersion() throws Exception {
    File root = tmp.newFolder("sdk");
    FileSdkStorage storage = new FileSdkStorage(root);
    storage.install("platforms;android-34", "2", zip("a/old.txt", "old"));
    try {
      storage.install("platforms;android-34", "3", zip("ok.txt", "x", "../evil.txt", "bad"));
      fail("installed");
    } catch (IOException expected) {
      // expected
    }
    assertEquals("2", storage.installed().get("platforms;android-34"));
    assertTrue(new File(root, "platforms/android-34/old.txt").isFile());
  }

  @Test
  public void removingDeletesThePackage() throws Exception {
    File root = tmp.newFolder("sdk");
    FileSdkStorage storage = new FileSdkStorage(root);
    storage.install("build-tools;34.0.0", "34.0.0", zip("x/f.txt", "f"));
    storage.remove("build-tools;34.0.0");
    assertTrue(storage.installed().isEmpty());
    assertFalse(new File(root, "build-tools/34.0.0").exists());
    storage.remove("build-tools;34.0.0");
  }

  @Test
  public void aFolderWithoutTheMarkerIsNotAnInstalledPackage() throws Exception {
    File root = tmp.newFolder("sdk");
    assertTrue(new File(root, "platforms/android-30").mkdirs());
    assertTrue(new FileSdkStorage(root).installed().isEmpty());
  }

  @Test
  public void pathsThatAreNotOfferedAreRefused() throws Exception {
    FileSdkStorage storage = new FileSdkStorage(tmp.newFolder("sdk"));
    String[] bad = {"emulator", "platforms;../x", "platforms;android-34/../../x", "build-tools;"};
    for (String path : bad) {
      try {
        storage.remove(path);
        fail("accepted " + path);
      } catch (IOException expected) {
        // expected
      }
    }
  }

  @Test
  public void anAcceptedLicenseIsRemembered() throws Exception {
    FileSdkStorage storage = new FileSdkStorage(tmp.newFolder("sdk"));
    assertNull(storage.acceptedLicenseHash("android-sdk-license"));
    storage.acceptLicense("android-sdk-license", "abc123");
    assertEquals("abc123", storage.acceptedLicenseHash("android-sdk-license"));
    storage.acceptLicense("android-sdk-license", "def456");
    assertEquals("def456", storage.acceptedLicenseHash("android-sdk-license"));
  }

  @Test
  public void aLicenseIdThatIsAPathIsRefused() throws Exception {
    FileSdkStorage storage = new FileSdkStorage(tmp.newFolder("sdk"));
    try {
      storage.acceptLicense("../../x", "h");
      fail("accepted");
    } catch (IOException expected) {
      // expected
    }
  }
}
