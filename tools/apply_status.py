"""One-off: wires the status line and undo/redo into EditorFragment and adds their strings."""
import pathlib

root = pathlib.Path("feature/editor/src/main")


def edit(path, pairs):
    p = root / path
    s = p.read_text(encoding="utf-8")
    for old, new in pairs:
        assert s.count(old) == 1, (path, old[:70])
        s = s.replace(old, new)
    p.write_text(s, encoding="utf-8")


METHODS = '''  /** Undo/redo buttons, and the language server state, caret position and encoding at the bottom. */
  private void setUpStatusLine() {
    binding.undo.setOnClickListener(
        v -> {
          if (binding != null) {
            binding.editor.undo();
          }
        });
    binding.redo.setOnClickListener(
        v -> {
          if (binding != null) {
            binding.editor.redo();
          }
        });
    binding.editor.subscribeAlways(SelectionChangeEvent.class, event -> updateStatus());
    binding.editor.subscribeAlways(ContentChangeEvent.class, event -> updateUndoRedo());
    updateStatus();
    updateUndoRedo();
  }

  /** Shows the bar only while a file is shown, with the caret position and the server state. */
  private void updateStatus() {
    if (binding == null) {
      return;
    }
    boolean shown = currentPath != null;
    int visibility = shown ? View.VISIBLE : View.GONE;
    binding.statusBar.setVisibility(visibility);
    binding.statusDivider.setVisibility(visibility);
    if (!shown) {
      return;
    }
    binding.statusPosition.setText(
        getString(
            R.string.status_position,
            binding.editor.getCursor().getLeftLine() + 1,
            binding.editor.getCursor().getLeftColumn() + 1));
    if (isJava(currentPath) && javaServer != null) {
      int label;
      switch (javaServer.state()) {
        case READY:
          label = R.string.status_java_ready;
          break;
        case MISSING:
          label = R.string.status_java_missing;
          break;
        case FAILED:
          label = R.string.status_java_failed;
          break;
        case STARTING:
          label = R.string.status_java_starting;
          break;
        default:
          label = R.string.status_java_off;
          break;
      }
      binding.statusServer.setText(label);
    } else {
      binding.statusServer.setText("");
    }
  }

  private void updateUndoRedo() {
    if (binding == null) {
      return;
    }
    boolean canUndo = currentPath != null && binding.editor.canUndo();
    boolean canRedo = currentPath != null && binding.editor.canRedo();
    binding.undo.setEnabled(canUndo);
    binding.undo.setAlpha(canUndo ? 1f : 0.38f);
    binding.redo.setEnabled(canRedo);
    binding.redo.setAlpha(canRedo ? 1f : 0.38f);
  }

'''

edit("java/org/codirex/asm/feature/editor/EditorFragment.java", [
    ("    setUpSelectionMenu();\n    binding.tabs", "    setUpSelectionMenu();\n    setUpStatusLine();\n    binding.tabs"),
    ("    if (state == JavaLanguageServer.State.MISSING && !installOffered) {",
     "    updateStatus();\n    if (state == JavaLanguageServer.State.MISSING && !installOffered) {"),
    ("    hideSelectionMenu();\n    if (!loading) {",
     "    hideSelectionMenu();\n    updateStatus();\n    updateUndoRedo();\n    if (!loading) {"),
    ("  private void say(int message) {", METHODS + "  private void say(int message) {"),
])

EN = '''
    <string name="status_undo">Undo</string>
    <string name="status_redo">Redo</string>
    <string name="status_position">Ln %1$d, Col %2$d</string>
    <string name="status_encoding">UTF-8</string>
    <string name="status_java_ready">Java ready</string>
    <string name="status_java_starting">Java starting…</string>
    <string name="status_java_missing">Java server not installed</string>
    <string name="status_java_failed">Java server stopped</string>
    <string name="status_java_off">Java server off</string>
</resources>'''
AR = '''
    <string name="status_undo">تراجع</string>
    <string name="status_redo">إعادة</string>
    <string name="status_position">س %1$d، ع %2$d</string>
    <string name="status_encoding">UTF-8</string>
    <string name="status_java_ready">Java جاهز</string>
    <string name="status_java_starting">Java يبدأ…</string>
    <string name="status_java_missing">خادم Java غير مثبّت</string>
    <string name="status_java_failed">توقّف خادم Java</string>
    <string name="status_java_off">خادم Java متوقف</string>
</resources>'''
edit("res/values/strings.xml", [("</resources>", EN.encode().decode("unicode_escape"))])
edit("res/values-ar/strings.xml", [("</resources>", AR.encode().decode("unicode_escape"))])
print("done")
