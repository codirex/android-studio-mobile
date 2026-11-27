package org.codirex.asm.domain.xmlassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The widgets and layouts of the framework that a layout file names, and which one extends which:
 * the styleables of a tag are those of its class and its superclasses. A tag that is not known (a
 * library or custom view) gets the attributes of View.
 */
public final class ViewClasses {

  private static final Map<String, String> PARENT = new LinkedHashMap<>();

  static {
    String[][] table = {
      {"ViewGroup", "View"},
      {"LinearLayout", "ViewGroup"},
      {"RelativeLayout", "ViewGroup"},
      {"FrameLayout", "ViewGroup"},
      {"GridLayout", "ViewGroup"},
      {"ScrollView", "FrameLayout"},
      {"HorizontalScrollView", "FrameLayout"},
      {"TableLayout", "LinearLayout"},
      {"TableRow", "LinearLayout"},
      {"RadioGroup", "LinearLayout"},
      {"TextView", "View"},
      {"Button", "TextView"},
      {"EditText", "TextView"},
      {"CheckedTextView", "TextView"},
      {"CompoundButton", "Button"},
      {"CheckBox", "CompoundButton"},
      {"RadioButton", "CompoundButton"},
      {"Switch", "CompoundButton"},
      {"ToggleButton", "CompoundButton"},
      {"ImageView", "View"},
      {"ImageButton", "ImageView"},
      {"ProgressBar", "View"},
      {"SeekBar", "ProgressBar"},
      {"AbsListView", "ViewGroup"},
      {"ListView", "AbsListView"},
      {"GridView", "AbsListView"},
      {"Spinner", "View"},
      {"WebView", "View"},
      {"ViewStub", "View"},
      {"Space", "View"},
    };
    for (String[] row : table) {
      PARENT.put(row[0], row[1]);
    }
  }

  private ViewClasses() {}

  /** The tag names to propose, {@code View} first. */
  public static List<String> knownTags() {
    List<String> tags = new ArrayList<>();
    tags.add("View");
    tags.addAll(PARENT.keySet());
    return tags;
  }

  /** The styleables of a tag: its class, then its superclasses up to View. */
  public static List<String> chain(String tag) {
    String name = tag.substring(tag.lastIndexOf('.') + 1);
    if (!name.equals("View") && !PARENT.containsKey(name)) {
      return Collections.singletonList("View");
    }
    List<String> out = new ArrayList<>();
    String current = name;
    while (current != null && !out.contains(current)) {
      out.add(current);
      current = PARENT.get(current);
    }
    return out;
  }

  /**
   * The styleables of the {@code layout_*} attributes of a child of {@code parent} (null for the root
   * element): the layout parameters of the parent's class and its superclasses, then the ones every
   * ViewGroup has.
   */
  public static List<String> layoutStyleables(String parent) {
    List<String> out = new ArrayList<>();
    if (parent != null) {
      for (String cls : chain(parent)) {
        out.add(cls + "_Layout");
      }
    }
    out.add("ViewGroup_Layout");
    out.add("ViewGroup_MarginLayout");
    return out;
  }
}
