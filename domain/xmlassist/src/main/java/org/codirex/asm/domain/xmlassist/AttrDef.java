package org.codirex.asm.domain.xmlassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An attribute of the Android framework as attrs.xml describes it: its name (without the {@code
 * android:} prefix), the formats it accepts (reference, dimension, boolean, enum, flag...) and, for
 * an enum or flag, its values. Immutable.
 */
public final class AttrDef {

  private final String name;
  private final List<String> formats;
  private final List<String> values;

  public AttrDef(String name, List<String> formats, List<String> values) {
    this.name = Objects.requireNonNull(name, "name");
    this.formats = Collections.unmodifiableList(new ArrayList<>(formats));
    this.values = Collections.unmodifiableList(new ArrayList<>(values));
  }

  public String name() {
    return name;
  }

  public List<String> formats() {
    return formats;
  }

  /** The names of the enum or flag values; empty for other formats. */
  public List<String> values() {
    return values;
  }

  /** Whether the attribute has neither a format nor values: a reference to a definition elsewhere. */
  boolean isBare() {
    return formats.isEmpty() && values.isEmpty();
  }
}
