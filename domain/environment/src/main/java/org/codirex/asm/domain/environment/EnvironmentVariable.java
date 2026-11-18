package org.codirex.asm.domain.environment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A variable the person sets for every shell of the terminal. Immutable; a value that exists is
 * valid.
 */
public final class EnvironmentVariable {

  /** Longest accepted value, in characters. */
  public static final int MAX_VALUE_LENGTH = 4096;

  /**
   * Names the terminal sets itself and needs to work: the home folder, the prefix, the shell and the
   * library that lets programs find their interpreters. Everything else can be changed.
   */
  private static final Set<String> RESERVED =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("HOME", "PREFIX", "SHELL", "LD_PRELOAD")));

  private final String name;
  private final String value;

  /**
   * @throws EnvironmentException if the name or the value is not acceptable
   */
  public EnvironmentVariable(String name, String value) throws EnvironmentException {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(value, "value");
    if (!isValidName(name)) {
      throw new EnvironmentException(EnvironmentException.Reason.INVALID_NAME, name);
    }
    if (isReserved(name)) {
      throw new EnvironmentException(EnvironmentException.Reason.RESERVED_NAME, name);
    }
    if (value.length() > MAX_VALUE_LENGTH || value.indexOf('\0') >= 0) {
      throw new EnvironmentException(EnvironmentException.Reason.INVALID_VALUE, name);
    }
    this.name = name;
    this.value = value;
  }

  /** Letters, digits and underscores, not starting with a digit: what a shell accepts. */
  public static boolean isValidName(String name) {
    if (name.isEmpty()) {
      return false;
    }
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      boolean letter = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
      boolean digit = c >= '0' && c <= '9';
      if (!letter && !(digit && i > 0)) {
        return false;
      }
    }
    return true;
  }

  /** Whether the terminal sets {@code name} itself and does not let it be changed. */
  public static boolean isReserved(String name) {
    return RESERVED.contains(name);
  }

  public String name() {
    return name;
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof EnvironmentVariable)) {
      return false;
    }
    EnvironmentVariable that = (EnvironmentVariable) other;
    return name.equals(that.name) && value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }

  @Override
  public String toString() {
    return "EnvironmentVariable(" + name + ")";
  }
}
