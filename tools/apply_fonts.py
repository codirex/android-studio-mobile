"""One-off: uses the font families in the theme, the code text appearances and the editor."""
import pathlib


def replace(path, old, new, count=1):
    p = pathlib.Path(path)
    s = p.read_text(encoding="utf-8")
    assert s.count(old) == count, (path, old[:80], s.count(old))
    p.write_text(s.replace(old, new), encoding="utf-8")


ANCHOR = '        <item name="shapeAppearanceSmallComponent">@style/ShapeAppearance.Asm.Small</item>'
FONT = ('        <item name="android:fontFamily">@font/inter</item>\n'
        '        <item name="fontFamily">@font/inter</item>\n')
for theme in ("core/ui/src/main/res/values/themes.xml", "core/ui/src/main/res/values-night/themes.xml"):
    replace(theme, ANCHOR, FONT + ANCHOR)

THEMES = "core/ui/src/main/res/values/themes.xml"
replace(THEMES,
        '        <item name="android:fontFamily">monospace</item>',
        '        <item name="android:fontFamily">@font/jetbrains_mono</item>\n'
        '        <item name="fontFamily">@font/jetbrains_mono</item>',
        count=2)
replace(THEMES, 'the platform monospace face.', 'JetBrains Mono.')

EDITOR = "feature/editor/src/main/java/org/codirex/asm/feature/editor/EditorFragment.java"
replace(EDITOR,
        '    binding.editor.registerDiagnosticProvider(diagnosticProvider);',
        '''    binding.editor.registerDiagnosticProvider(diagnosticProvider);
    android.graphics.Typeface mono =
        androidx.core.content.res.ResourcesCompat.getFont(
            requireContext(), org.codirex.asm.core.ui.R.font.jetbrains_mono_regular);
    if (mono != null) {
      binding.editor.setTypefaceText(mono);
    }''')

adr = pathlib.Path("docs/adr/0034-design-system.md")
adr.write_text(adr.read_text(encoding="utf-8").rstrip("\n") + '''

## Fonts

The design's two families are bundled in `core:ui` (`res/font`, SIL OFL 1.1, see
`docs/third-party/fonts.md`): Inter for the interface (set in the theme) and JetBrains Mono for
code (the `TextAppearance.Asm.Code*` styles and the editor widget). Noto Sans Arabic is not
bundled: Arabic text uses the system face. The terminal view keeps the platform monospace.
''', encoding="utf-8")
print("done")
