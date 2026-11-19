package org.codirex.asm.domain.sdk;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/** A license a package falls under. The person accepts it before installing. Immutable. */
public final class SdkLicense {

  private final String id;
  private final String text;

  public SdkLicense(String id, String text) {
    this.id = Objects.requireNonNull(id, "id");
    this.text = Objects.requireNonNull(text, "text");
  }

  public String id() {
    return id;
  }

  public String text() {
    return text;
  }

  /**
   * The SHA-1 of the text, with line ends normalized and the ends trimmed. What the person accepted
   * is stored as this hash, so a changed text asks for a new acceptance.
   */
  public String hash() {
    String normalized = text.replace("\r\n", "\n").trim();
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-1").digest(normalized.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        hex.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
