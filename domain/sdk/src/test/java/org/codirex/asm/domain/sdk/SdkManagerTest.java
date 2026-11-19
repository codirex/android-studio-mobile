package org.codirex.asm.domain.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class SdkManagerTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private static final byte[] CONTENT = "pretend zip".getBytes(StandardCharsets.UTF_8);

  private static String sha1(byte[] bytes) throws Exception {
    StringBuilder hex = new StringBuilder();
    for (byte b : MessageDigest.getInstance("SHA-1").digest(bytes)) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }

  private static String catalogXml(String licenseText, String sha) {
    StringBuilder xml = new StringBuilder("<r><license id=\"lic\">" + licenseText + "</license>");
    String[] paths = {
      "build-tools;9.0.0", "platforms;android-9", "platforms;android-34", "build-tools;34.0.0",
      "platforms;android-34-ext10", "build-tools;34.0.0-rc1", "platform-tools", "emulator"
    };
    for (String path : paths) {
      xml.append("<remotePackage path=\"").append(path).append("\"><revision><major>1</major></revision>")
          .append("<uses-license ref=\"lic\"/><archives><archive><complete><size>")
          .append(CONTENT.length).append("</size><checksum type=\"sha1\">").append(sha)
          .append("</checksum><url>").append(path.replace(';', '_')).append(".zip</url></complete></archive></archives></remotePackage>");
    }
    return xml.append("</r>").toString();
  }

  private final class FakeSource implements SdkSource {
    String xml;
    byte[] served = CONTENT;
    boolean cancelDuringDownload;
    int downloads;
    File lastFile;

    @Override
    public InputStream openCatalog() {
      return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public File download(SdkArchive archive, TransferProgress progress) throws IOException {
      downloads++;
      if (cancelDuringDownload) {
        throw new InterruptedIOException("cancelled");
      }
      progress.onProgress(served.length, served.length);
      lastFile = tmp.newFile();
      Files.write(lastFile.toPath(), served);
      return lastFile;
    }
  }

  private static final class FakeStorage implements SdkStorage {
    final Map<String, String> installed = new HashMap<>();
    final Map<String, String> licenses = new HashMap<>();
    boolean failing;

    @Override
    public Map<String, String> installed() {
      return installed;
    }

    @Override
    public void install(String path, String revision, File archive) throws IOException {
      if (failing) {
        throw new IOException("disk");
      }
      installed.put(path, revision);
    }

    @Override
    public void remove(String path) {
      installed.remove(path);
    }

    @Override
    public String acceptedLicenseHash(String licenseId) {
      return licenses.get(licenseId);
    }

    @Override
    public void acceptLicense(String licenseId, String hash) {
      licenses.put(licenseId, hash);
    }
  }

  private FakeSource source() throws Exception {
    FakeSource source = new FakeSource();
    source.xml = catalogXml("License text", sha1(CONTENT));
    return source;
  }

  @Test
  public void onlyPlatformsAndReleasedBuildToolsAreOffered() {
    assertTrue(SdkManager.isOffered("platforms;android-34"));
    assertTrue(SdkManager.isOffered("platforms;android-33-ext4"));
    assertTrue(SdkManager.isOffered("build-tools;34.0.0"));
    assertFalse(SdkManager.isOffered("build-tools;35.0.0-rc1"));
    assertFalse(SdkManager.isOffered("platforms;android-VanillaIceCream"));
    assertFalse(SdkManager.isOffered("emulator"));
    assertFalse(SdkManager.isOffered("platform-tools"));
    assertFalse(SdkManager.isOffered("platforms;android-34/../x"));
  }

  @Test
  public void theListIsFilteredAndSortedPlatformsFirstNewestFirst() throws Exception {
    List<SdkEntry> list = new SdkManager(source(), new FakeStorage()).list();
    String[] expected = {
      "platforms;android-34-ext10", "platforms;android-34", "platforms;android-9",
      "build-tools;34.0.0", "build-tools;9.0.0"
    };
    assertEquals(expected.length, list.size());
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], list.get(i).pkg().path());
    }
  }

  @Test
  public void theListShowsWhatIsInstalledAndAccepted() throws Exception {
    FakeStorage storage = new FakeStorage();
    storage.installed.put("platforms;android-34", "0");
    SdkManager manager = new SdkManager(source(), storage);
    SdkEntry entry = manager.list().get(1);
    assertEquals("platforms;android-34", entry.pkg().path());
    assertTrue(entry.isInstalled());
    assertTrue(entry.isUpdatable());
    assertFalse(entry.isLicenseAccepted());
    manager.acceptLicense(entry.pkg().license());
    assertTrue(manager.list().get(1).isLicenseAccepted());
  }

  @Test
  public void installingNeedsTheLicenseAndDownloadsNothingWithout() throws Exception {
    FakeSource source = source();
    SdkManager manager = new SdkManager(source, new FakeStorage());
    SdkPackage pkg = manager.list().get(0).pkg();
    try {
      manager.install(pkg, TransferProgress.NONE);
      fail("installed");
    } catch (SdkException e) {
      assertEquals(SdkException.Reason.LICENSE_NOT_ACCEPTED, e.reason());
    }
    assertEquals(0, source.downloads);
  }

  @Test
  public void aChangedLicenseTextAsksAgain() throws Exception {
    FakeStorage storage = new FakeStorage();
    FakeSource source = source();
    SdkManager manager = new SdkManager(source, storage);
    manager.acceptLicense(manager.list().get(0).pkg().license());
    assertTrue(manager.list().get(0).isLicenseAccepted());
    source.xml = catalogXml("License text, new version", sha1(CONTENT));
    assertFalse(manager.list().get(0).isLicenseAccepted());
  }

  @Test
  public void anAcceptedPackageIsDownloadedVerifiedInstalledAndCleanedUp() throws Exception {
    FakeStorage storage = new FakeStorage();
    FakeSource source = source();
    SdkManager manager = new SdkManager(source, storage);
    SdkPackage pkg = manager.list().get(1).pkg();
    manager.acceptLicense(pkg.license());
    manager.install(pkg, TransferProgress.NONE);
    assertEquals("1", storage.installed.get("platforms;android-34"));
    assertFalse(source.lastFile.exists());
    manager.remove("platforms;android-34");
    assertNull(storage.installed.get("platforms;android-34"));
  }

  @Test
  public void aWrongChecksumInstallsNothingAndKeepsNoFile() throws Exception {
    FakeStorage storage = new FakeStorage();
    FakeSource source = source();
    source.served = "tampered".getBytes(StandardCharsets.UTF_8);
    SdkManager manager = new SdkManager(source, storage);
    SdkPackage pkg = manager.list().get(0).pkg();
    manager.acceptLicense(pkg.license());
    try {
      manager.install(pkg, TransferProgress.NONE);
      fail("installed");
    } catch (SdkException e) {
      assertEquals(SdkException.Reason.CHECKSUM, e.reason());
    }
    assertTrue(storage.installed.isEmpty());
    assertFalse(source.lastFile.exists());
  }

  @Test
  public void aCancelledDownloadIsACancellation() throws Exception {
    FakeSource source = source();
    source.cancelDuringDownload = true;
    SdkManager manager = new SdkManager(source, new FakeStorage());
    SdkPackage pkg = manager.list().get(0).pkg();
    manager.acceptLicense(pkg.license());
    try {
      manager.install(pkg, TransferProgress.NONE);
      fail("installed");
    } catch (SdkException e) {
      assertEquals(SdkException.Reason.CANCELLED, e.reason());
    }
  }

  @Test
  public void aPackageThatIsNotOfferedIsRefusedEvenWithItsLicense() throws Exception {
    SdkManager manager = new SdkManager(source(), new FakeStorage());
    SdkPackage listed = manager.list().get(0).pkg();
    SdkPackage emulator = new SdkPackage("emulator", "Emulator", "1", listed.license(), listed.archive());
    try {
      manager.install(emulator, TransferProgress.NONE);
      fail("installed");
    } catch (SdkException e) {
      assertEquals(SdkException.Reason.NOT_OFFERED, e.reason());
    }
  }

  @Test
  public void aFailingStorageIsReportedAndTheDownloadIsRemoved() throws Exception {
    FakeStorage storage = new FakeStorage();
    FakeSource source = source();
    SdkManager manager = new SdkManager(source, storage);
    SdkPackage pkg = manager.list().get(0).pkg();
    manager.acceptLicense(pkg.license());
    storage.failing = true;
    try {
      manager.install(pkg, TransferProgress.NONE);
      fail("installed");
    } catch (SdkException e) {
      assertEquals(SdkException.Reason.STORAGE, e.reason());
    }
    assertFalse(source.lastFile.exists());
  }
}
