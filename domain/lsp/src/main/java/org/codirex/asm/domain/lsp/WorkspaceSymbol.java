package org.codirex.asm.domain.lsp;

import java.util.Objects;

/**
 * A symbol found anywhere in the workspace (a class, a method, a field...): its name, the symbol it
 * is inside of, and where it is declared (the file as a URI, zero based line and character). Immutable.
 */
public final class WorkspaceSymbol {

  private final String name;
  private final int kind;
  private final String container;
  private final String uri;
  private final int line;
  private final int character;

  public WorkspaceSymbol(
      String name, int kind, String container, String uri, int line, int character) {
    this.name = Objects.requireNonNull(name, "name");
    this.kind = kind;
    this.container = Objects.requireNonNull(container, "container");
    this.uri = Objects.requireNonNull(uri, "uri");
    this.line = line;
    this.character = character;
  }

  public String name() {
    return name;
  }

  public int kind() {
    return kind;
  }

  /** The type or package the symbol is inside of; may be empty. */
  public String container() {
    return container;
  }

  public String uri() {
    return uri;
  }

  /** Zero based; 0 when the server only named the file. */
  public int line() {
    return line;
  }

  /** Zero based; 0 when the server only named the file. */
  public int character() {
    return character;
  }

  /** A short word for the kind: class, method, field... */
  public String kindName() {
    return new DocumentSymbol(name, "", kind, 0, 0, "").kindName();
  }
}
