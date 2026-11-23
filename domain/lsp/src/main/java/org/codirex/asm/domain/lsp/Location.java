package org.codirex.asm.domain.lsp;

import java.util.Objects;

/** A place in a document the server points at: its address and the start of the range (zero based). Immutable. */
public final class Location {

  private final String uri;
  private final int line;
  private final int character;

  public Location(String uri, int line, int character) {
    this.uri = Objects.requireNonNull(uri, "uri");
    this.line = line;
    this.character = character;
  }

  public String uri() {
    return uri;
  }

  /** Zero based. */
  public int line() {
    return line;
  }

  /** Zero based. */
  public int character() {
    return character;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Location
        && uri.equals(((Location) other).uri)
        && line == ((Location) other).line
        && character == ((Location) other).character;
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, line, character);
  }

  @Override
  public String toString() {
    return uri + ":" + line + ":" + character;
  }
}
