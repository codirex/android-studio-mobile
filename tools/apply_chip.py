"""One-off: shows the open project as a chip in the top bar."""
import pathlib

PATH = pathlib.Path("app/src/main/java/org/codirex/asm/MainActivity.java")
s = PATH.read_text(encoding="utf-8")


def replace(old, new):
    global s
    assert s.count(old) == 1, (old[:80], s.count(old))
    s = s.replace(old, new)


replace("import android.view.Menu;", "import android.view.Menu;\nimport android.view.View;")
replace("    setSupportActionBar(binding.toolbar);\n",
        "    setSupportActionBar(binding.toolbar);\n    binding.projectChip.setOnClickListener(v -> showPalette());\n")
replace("      bar.setTitle(open != null ? open.project().value() : getString(R.string.app_name));\n",
        "      bar.setTitle(open != null ? \"\" : getString(R.string.app_name));\n"
        "      binding.projectChip.setVisibility(open != null ? View.VISIBLE : View.GONE);\n"
        "      if (open != null) {\n"
        "        binding.projectChip.setText(open.project().value());\n"
        "      }\n")
PATH.write_text(s, encoding="utf-8")
print("done")
