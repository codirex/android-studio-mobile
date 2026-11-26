package org.codirex.asm.domain.lsp;

import java.util.Objects;

/**
 * A symbol of a document (a class, a method, a field...) as the server lists it: its name, where it
 * is declared (zero based) and the symbol it is inside of. Immutable.
 */
public final class DocumentSymbol {

  private final String name;
  private final String detail;
  private final int kind;
  private final int line;
  private final int character;
  private final String container;

  public DocumentSymbol(
      String name, String detail, int kind, int line, int character, String container) {
    this.name = Objects.requireNonNull(name, "name");
    this.detail = Objects.requireNonNull(detail, "detail");
    this.kind = kind;
    this.line = line;
    this.character = character;
    this.container = Objects.requireNonNull(container, "container");
  }

  public String name() {
    return name;
  }

  /** More about the symbol, such as its signature; may be empty. */
  public String detail() {
    return detail;
  }

  /** The LSP symbol kind number. */
  public int kind() {
    return kind;
  }

  /** Where the name starts, zero based. */
  public int line() {
    return line;
  }

  /** Where the name starts, zero based. */
  public int character() {
    return character;
  }

  /** The name of the symbol this one is inside of; empty at the top. */
  public String container() {
    return container;
  }

  /** A short word for the kind: class, method, field... */
  public String kindName() {
    switch (kind) {
      case 1:
        return "file";
      case 2:
        return "module";
      case 3:
        return "namespace";
      case 4:
        return "package";
      case 5:
        return "class";
      case 6:
        return "method";
      case 7:
        return "property";
      case 8:
        return "field";
      case 9:
        return "constructor";
      case 10:
        return "enum";
      case 11:
        return "interface";
      case 12:
        return "function";
      case 13:
        return "variable";
      case 14:
        return "constant";
      case 22:
        return "enum member";
      case 23:
        return "struct";
      case 26:
        return "type parameter";
      default:
        return "symbol";
    }
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof DocumentSymbol)) {
      return false;
    }
    DocumentSymbol o = (DocumentSymbol) other;
    return name.equals(o.name)
        && detail.equals(o.detail)
        && kind == o.kind
        && line == o.line
        && character == o.character
        && container.equals(o.container);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, detail, kind, line, character, container);
  }

  @Override
  public String toString() {
    return kindName() + " " + name + " at " + line + ":" + character;
  }
}
