package org.codirex.asm.domain.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public final class CatalogParserTest {

  private static final String SHA_A = "0123456789abcdef0123456789abcdef01234567";
  private static final String SHA_B = "fedcba9876543210fedcba9876543210fedcba98";

  /** The shape of the Google catalog (repository2-3.xml), cut down to what the parser reads. */
  private static final String SAMPLE =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
          + "<sdk:sdk-repository xmlns:sdk=\"http://schemas.android.com/sdk/android/repo/repository2/03\""
          + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
          + "<license id=\"android-sdk-license\" type=\"text\">Terms and Conditions\r\nline two</license>\n"
          + "<channel id=\"channel-0\">Stable</channel>\n"
          + "<remotePackage path=\"platforms;android-34\">\n"
          + "  <type-details xsi:type=\"sdk:platformDetailsType\"><api-level>34</api-level></type-details>\n"
          + "  <revision><major>3</major></revision>\n"
          + "  <display-name>Android SDK Platform 34</display-name>\n"
          + "  <uses-license ref=\"android-sdk-license\"/>\n"
          + "  <channelRef ref=\"channel-0\"/>\n"
          + "  <archives><archive><complete><size>65000000</size>"
          + "<checksum type=\"sha1\">" + SHA_A + "</checksum><url>platform-34_r03.zip</url></complete></archive></archives>\n"
          + "</remotePackage>\n"
          + "<remotePackage path=\"build-tools;34.0.0\">\n"
          + "  <revision><major>34</major><minor>0</minor><micro>0</micro></revision>\n"
          + "  <display-name>Android SDK Build-Tools 34</display-name>\n"
          + "  <uses-license ref=\"android-sdk-license\"/>\n"
          + "  <archives>\n"
          + "   <archive><complete><size>1</size><checksum type=\"sha1\">" + SHA_B + "</checksum><url>build-tools_r34-macosx.zip</url></complete><host-os>macosx</host-os></archive>\n"
          + "   <archive><complete><size>2</size><checksum type=\"sha1\">" + SHA_A + "</checksum><url>build-tools_r34-linux.zip</url></complete><host-os>linux</host-os></archive>\n"
          + "   <archive><complete><size>3</size><checksum type=\"sha1\">" + SHA_B + "</checksum><url>build-tools_r34-windows.zip</url></complete><host-os>windows</host-os></archive>\n"
          + "  </archives>\n"
          + "</remotePackage>\n"
          + "<remotePackage path=\"build-tools;35.0.0-rc1\">\n"
          + "  <revision><major>35</major><minor>0</minor><micro>0</micro><preview>1</preview></revision>\n"
          + "  <display-name>Android SDK Build-Tools 35-rc1</display-name>\n"
          + "  <uses-license ref=\"android-sdk-license\"/>\n"
          + "  <archives><archive><complete><size>1</size><checksum type=\"sha1\">" + SHA_A + "</checksum><url>x.zip</url></complete></archive></archives>\n"
          + "</remotePackage>\n"
          + "<remotePackage path=\"broken;no-license\">\n"
          + "  <revision><major>1</major></revision>\n"
          + "  <archives><archive><complete><size>1</size><checksum type=\"sha1\">" + SHA_A + "</checksum><url>y.zip</url></complete></archive></archives>\n"
          + "</remotePackage>\n"
          + "<remotePackage path=\"broken;bad-checksum\">\n"
          + "  <revision><major>1</major></revision>\n"
          + "  <uses-license ref=\"android-sdk-license\"/>\n"
          + "  <archives><archive><complete><size>1</size><checksum type=\"sha1\">short</checksum><url>z.zip</url></complete></archive></archives>\n"
          + "</remotePackage>\n"
          + "</sdk:sdk-repository>\n";

  private static SdkCatalog parse(String xml) throws Exception {
    return CatalogParser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void aPlatformIsReadWithItsArchiveAndLicense() throws Exception {
    SdkPackage platform = parse(SAMPLE).find("platforms;android-34");
    assertNotNull(platform);
    assertEquals("Android SDK Platform 34", platform.displayName());
    assertEquals("3", platform.revision());
    assertEquals("platform-34_r03.zip", platform.archive().url());
    assertEquals(SHA_A, platform.archive().sha1());
    assertEquals(65000000L, platform.archive().size());
    assertEquals("android-sdk-license", platform.license().id());
    assertTrue(platform.license().text().startsWith("Terms and Conditions"));
  }

  @Test
  public void buildToolsTakeTheLinuxArchiveAndAFullRevision() throws Exception {
    SdkPackage tools = parse(SAMPLE).find("build-tools;34.0.0");
    assertEquals("34.0.0", tools.revision());
    assertEquals("build-tools_r34-linux.zip", tools.archive().url());
  }

  @Test
  public void aPreviewRevisionIsMarked() throws Exception {
    assertEquals("35.0.0-rc1", parse(SAMPLE).find("build-tools;35.0.0-rc1").revision());
  }

  @Test
  public void incompleteEntriesAreLeftOutAndTheRestIsKept() throws Exception {
    SdkCatalog catalog = parse(SAMPLE);
    assertNull(catalog.find("broken;no-license"));
    assertNull(catalog.find("broken;bad-checksum"));
    assertEquals(3, catalog.packages().size());
  }

  @Test
  public void aDocumentThatIsNotXmlIsACatalogError() throws Exception {
    try {
      parse("this is not xml");
      fail("parsed");
    } catch (SdkException e) {
      assertEquals(SdkException.Reason.CATALOG, e.reason());
    }
  }

  @Test
  public void aDoctypeIsRefusedWhereTheParserCanSayIt() throws Exception {
    String evil = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><a>&e;</a>";
    try {
      SdkCatalog catalog = parse(evil);
      assertTrue(catalog.packages().isEmpty());
    } catch (SdkException e) {
      assertEquals(SdkException.Reason.CATALOG, e.reason());
    }
  }

  @Test
  public void theLicenseHashIgnoresLineEndsAndOuterWhitespace() {
    SdkLicense a = new SdkLicense("l", "  one\r\ntwo \n");
    SdkLicense b = new SdkLicense("l", "one\ntwo");
    assertEquals(a.hash(), b.hash());
    assertEquals(40, a.hash().length());
  }
}
