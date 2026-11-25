"""One-off: wires the breadcrumb and the quick key bar into EditorFragment."""
import pathlib

p = pathlib.Path("feature/editor/src/main/java/org/codirex/asm/feature/editor/EditorFragment.java")
s = p.read_text(encoding="utf-8")

METHOD = '''  /** The symbols that are slow to reach on a phone keyboard; Tab inserts four spaces. */
  private void setUpQuickKeys() {
    String[] keys = {
      "Tab", "{", "}", "(", ")", "[", "]", ";", "\\"", "'", "=", "<", ">", "/", "!", ".", ","
    };
    LayoutInflater inflater = getLayoutInflater();
    for (String key : keys) {
      android.widget.TextView view =
          (android.widget.TextView)
              inflater.inflate(R.layout.item_quick_key, binding.quickKeys, false);
      view.setText(key);
      final String text = key.equals("Tab") ? "    " : key;
      view.setOnClickListener(
          v -> {
            if (binding != null && currentPath != null) {
              binding.editor.insertText(text, text.length());
            }
          });
      binding.quickKeys.addView(view);
    }
  }

'''

pairs = [
    ("    setUpStatusLine();\n    binding.tabs", "    setUpStatusLine();\n    setUpQuickKeys();\n    binding.tabs"),
    ("    binding.statusDivider.setVisibility(visibility);\n",
     "    binding.statusDivider.setVisibility(visibility);\n    binding.quickKeysScroll.setVisibility(visibility);\n    binding.breadcrumb.setVisibility(visibility);\n    binding.breadcrumb.setText(shown ? currentPath.replace(\"/\", \"  \\u203A  \") : \"\");\n"),
    ("  private void say(int message) {", METHOD + "  private void say(int message) {"),
]
for old, new in pairs:
    assert s.count(old) == 1, old[:70]
    s = s.replace(old, new)
p.write_text(s, encoding="utf-8")
print("done")
