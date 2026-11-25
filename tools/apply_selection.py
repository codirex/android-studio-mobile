"""One-off: edits EditorFragment, fragment_editor.xml and the strings for the selection menu."""
import pathlib

root = pathlib.Path("feature/editor/src/main")


def edit(path, pairs):
    p = root / path
    s = p.read_text(encoding="utf-8")
    for old, new in pairs:
        assert s.count(old) == 1, (path, old[:60])
        s = s.replace(old, new)
    p.write_text(s, encoding="utf-8")


java = "java/org/codirex/asm/feature/editor/EditorFragment.java"
s = (root / java).read_text(encoding="utf-8")
i = s.index("  /** The buttons for definition, information and format")
j = s.index("  private void say(int message)")
NEW = pathlib.Path("tools/selection_methods.txt").read_text(encoding="utf-8")
s = s[:i] + NEW + s[j:]
(root / java).write_text(s, encoding="utf-8")
edit(java, [
    ("import io.github.rosemoe.sora.event.ContentChangeEvent;",
     "import io.github.rosemoe.sora.event.ContentChangeEvent;\nimport io.github.rosemoe.sora.event.ScrollEvent;\nimport io.github.rosemoe.sora.event.SelectionChangeEvent;"),
    ("import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;",
     "import io.github.rosemoe.sora.widget.component.EditorTextActionWindow;\nimport io.github.rosemoe.sora.widget.schemes.SchemeDarcula;"),
    ("  private JavaLanguageServer javaServer;\n",
     "  private JavaLanguageServer javaServer;\n\n  /** The menu above a selection (copy, paste, definition...), or null before the view exists. */\n  private SelectionMenu selectionMenu;\n"),
    ("    binding.javaDefinition.setOnClickListener(v -> goToDefinition());\n    binding.javaHover.setOnClickListener(v -> showHover());\n    binding.javaFormat.setOnClickListener(v -> formatDocument());\n",
     "    setUpSelectionMenu();\n"),
    ("    binding.editor.unregisterDiagnosticProvider(diagnosticProvider);\n    binding.editor.release();",
     "    binding.editor.unregisterDiagnosticProvider(diagnosticProvider);\n    if (selectionMenu != null) {\n      selectionMenu.dismiss();\n      selectionMenu = null;\n    }\n    binding.editor.release();"),
    ("    updateJavaTools();\n    if (state == JavaLanguageServer.State.MISSING", "    if (state == JavaLanguageServer.State.MISSING"),
    ("    updateJavaTools();\n    if (!loading) {", "    hideSelectionMenu();\n    if (!loading) {"),
])

layout = root / "res/layout/fragment_editor.xml"
t = layout.read_text(encoding="utf-8")
a = t.index("    <HorizontalScrollView")
b = t.index("</HorizontalScrollView>") + len("</HorizontalScrollView>\n\n")
layout.write_text(t[:a] + t[b:], encoding="utf-8")

edit("res/values/strings.xml", [("</resources>",
    '\n    <string name="editor_action_cut">Cut</string>\n    <string name="editor_action_copy">Copy</string>\n    <string name="editor_action_paste">Paste</string>\n    <string name="editor_action_select_all">Select all</string>\n</resources>')])
edit("res/values-ar/strings.xml", [("</resources>",
    '\n    <string name="editor_action_cut">قص</string>\n    <string name="editor_action_copy">نسخ</string>\n    <string name="editor_action_paste">لصق</string>\n    <string name="editor_action_select_all">تحديد الكل</string>\n</resources>')])
print("done")
