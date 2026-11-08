package org.codirex.asm.core.preferences;

import java.util.Objects;

/**
 * A typed preference key with its default value.
 *
 * <p>Declare keys once, as constants next to the feature that owns them, and pass them to a {@link
 * PreferenceStore}. The type of the key decides how the value is stored, so a key can never be read
 * as the wrong type.
 *
 * @param <T> {@link Boolean}, {@link Integer} or {@link String}
 */
public final class Pref<T> {

  enum Type {
    BOOLEAN,
    INT,
    STRING
  }

  private final String key;
  private final T defaultValue;
  private final Type type;

  private Pref(String key, T defaultValue, Type type) {
    this.key = Objects.requireNonNull(key, "key");
    this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
    this.type = type;
  }

  /** A true/false preference. */
  public static Pref<Boolean> ofBoolean(String key, boolean defaultValue) {
    return new Pref<>(key, defaultValue, Type.BOOLEAN);
  }

  /** A whole-number preference. */
  public static Pref<Integer> ofInt(String key, int defaultValue) {
    return new Pref<>(key, defaultValue, Type.INT);
  }

  /** A text preference. The default may not be null. */
  public static Pref<String> ofString(String key, String defaultValue) {
    return new Pref<>(key, defaultValue, Type.STRING);
  }

  /** The name under which the value is stored. */
  public String key() {
    return key;
  }

  /** The value used while nothing is stored. */
  public T defaultValue() {
    return defaultValue;
  }

  Type type() {
    return type;
  }

  @Override
  public String toString() {
    return "Pref(" + key + ")";
  }
}
