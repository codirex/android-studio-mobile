package org.codirex.asm.domain.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Reads the repository catalog of the Android SDK ({@code repository2-3.xml}). A package whose
 * entry is incomplete (no license, no archive) is left out, not an error: the catalog lists
 * thousands of entries and one odd one must not hide the others.
 *
 * <p>The archive: one that fits every host if there is one, else the one for Linux. The archives
 * for Linux hold programs for x86-64, which the device cannot run; they are still taken, because
 * the same archives hold the libraries (jar files) and the layout builds expect.
 */
public final class CatalogParser {

  private CatalogParser() {}

  public static SdkCatalog parse(InputStream in) throws SdkException {
    Document document = read(in);
    Map<String, SdkLicense> licenses = new HashMap<>();
    NodeList licenseNodes = document.getElementsByTagName("license");
    for (int i = 0; i < licenseNodes.getLength(); i++) {
      Element element = (Element) licenseNodes.item(i);
      String id = element.getAttribute("id");
      if (!id.isEmpty()) {
        licenses.put(id, new SdkLicense(id, element.getTextContent()));
      }
    }
    List<SdkPackage> packages = new ArrayList<>();
    NodeList packageNodes = document.getElementsByTagName("remotePackage");
    for (int i = 0; i < packageNodes.getLength(); i++) {
      SdkPackage pkg = readPackage((Element) packageNodes.item(i), licenses);
      if (pkg != null) {
        packages.add(pkg);
      }
    }
    return new SdkCatalog(packages);
  }

  private static Document read(InputStream in) throws SdkException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      try {
        // The catalog has no DTD; refuse one (external entities) where the parser can say so.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      } catch (ParserConfigurationException e) {
        // Not every parser knows the feature.
      }
      DocumentBuilder builder = factory.newDocumentBuilder();
      // The default handler prints parse errors to the standard error; the caller gets them.
      builder.setErrorHandler(
          new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) {}

            @Override
            public void error(SAXParseException e) throws SAXException {
              throw e;
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
              throw e;
            }
          });
      return builder.parse(in);
    } catch (ParserConfigurationException | SAXException e) {
      throw new SdkException(SdkException.Reason.CATALOG, "unreadable catalog", e);
    } catch (IOException e) {
      throw new SdkException(SdkException.Reason.NETWORK, "catalog not read", e);
    }
  }

  private static SdkPackage readPackage(Element element, Map<String, SdkLicense> licenses) {
    String path = element.getAttribute("path");
    if (path.isEmpty()) {
      return null;
    }
    Element licenseRef = child(element, "uses-license");
    SdkLicense license = licenseRef == null ? null : licenses.get(licenseRef.getAttribute("ref"));
    SdkArchive archive = chooseArchive(element);
    if (license == null || archive == null) {
      return null;
    }
    String name = text(child(element, "display-name"));
    return new SdkPackage(
        path, name.isEmpty() ? path : name, revisionOf(child(element, "revision")), license, archive);
  }

  private static SdkArchive chooseArchive(Element packageElement) {
    Element archives = child(packageElement, "archives");
    if (archives == null) {
      return null;
    }
    SdkArchive linux = null;
    for (Node node = archives.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (!(node instanceof Element) || !"archive".equals(node.getNodeName())) {
        continue;
      }
      Element archive = (Element) node;
      SdkArchive parsed = archiveOf(archive);
      if (parsed == null) {
        continue;
      }
      String hostOs = text(child(archive, "host-os"));
      if (hostOs.isEmpty()) {
        return parsed;
      }
      if (hostOs.equals("linux") && linux == null) {
        linux = parsed;
      }
    }
    return linux;
  }

  private static SdkArchive archiveOf(Element archive) {
    Element complete = child(archive, "complete");
    if (complete == null) {
      return null;
    }
    String url = text(child(complete, "url"));
    String sha1 = text(child(complete, "checksum"));
    String size = text(child(complete, "size"));
    if (url.isEmpty() || sha1.length() != 40) {
      return null;
    }
    try {
      return new SdkArchive(url, sha1, size.isEmpty() ? -1 : Long.parseLong(size));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** {@code major[.minor[.micro]]}, as far as the catalog states it. */
  private static String revisionOf(Element revision) {
    if (revision == null) {
      return "0";
    }
    StringBuilder result = new StringBuilder(text(child(revision, "major")));
    String minor = text(child(revision, "minor"));
    String micro = text(child(revision, "micro"));
    if (!minor.isEmpty()) {
      result.append('.').append(minor);
      if (!micro.isEmpty()) {
        result.append('.').append(micro);
      }
    } else if (!micro.isEmpty()) {
      result.append(".0.").append(micro);
    }
    String preview = text(child(revision, "preview"));
    if (!preview.isEmpty()) {
      result.append("-rc").append(preview);
    }
    return result.length() == 0 ? "0" : result.toString();
  }

  private static Element child(Element parent, String name) {
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element && name.equals(node.getNodeName())) {
        return (Element) node;
      }
    }
    return null;
  }

  private static String text(Element element) {
    return element == null ? "" : element.getTextContent().trim();
  }
}
