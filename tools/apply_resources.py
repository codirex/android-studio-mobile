"""One-off: the editor gives the resources of the project to the Android completer."""
import pathlib

EDITOR = pathlib.Path("feature/editor/src/main/java/org/codirex/asm/feature/editor/EditorFragment.java")
s = EDITOR.read_text(encoding="utf-8")


def replace(old, new):
    global s
    assert s.count(old) == 1, (old[:80], s.count(old))
    s = s.replace(old, new)


replace("import org.codirex.asm.domain.xmlassist.AttrCatalog;",
        "import org.codirex.asm.domain.xmlassist.AttrCatalog;\nimport org.codirex.asm.domain.xmlassist.ResourceIndex;")
replace("new AndroidXmlCompleter(catalog).complete(text, offset)",
        "new AndroidXmlCompleter(catalog, resourcesNow()).complete(text, offset)")
replace("  private void renderTabs(EditorState state) {",
        '''  /** The resources of the project, read again when the last reading is a few seconds old. */
  private ResourceIndex resourcesNow() {
    synchronized (resourceLock) {
      long now = System.currentTimeMillis();
      if (resources == null || now - resourcesAt > RESOURCES_TTL_MS) {
        resources = ResourceIndex.scan(projectDirectory);
        resourcesAt = now;
      }
      return resources;
    }
  }

  private void renderTabs(EditorState state) {''')
replace("  private static final long SERVER_SYNC_DELAY_MS = 400;",
        '''  private static final long SERVER_SYNC_DELAY_MS = 400;
  private static final long RESOURCES_TTL_MS = 5000;''')
replace("  private File projectDirectory;",
        '''  private File projectDirectory;
  private final Object resourceLock = new Object();
  private ResourceIndex resources;
  private long resourcesAt;''')
EDITOR.write_text(s, encoding="utf-8")

adr = pathlib.Path("docs/adr/0036-android-xml-completion.md")
text = adr.read_text(encoding="utf-8")
old = "No platform installed means no proposals of this kind. `app:` attributes, library views and project\nresources (`@string/...` names) are not proposed yet: they need the resource index of the project."
assert text.count(old) == 1
adr.write_text(text.replace(old,
    "No platform installed means no proposals of this kind. `app:` attributes and library views are not\nproposed yet.\n\n## Resources of the project\n\n`ResourceIndex` lists the names of the project's own resources: values files (`string`, `color`,\n`dimen`, `style`...), the file names in the other `res` folders (`drawable`, `layout`...) and the ids\nthat layouts use. Value completion offers `@type/` roots and, after the slash, those names for any\nattribute that accepts a reference, string, color or dimension. The editor reads the index again when\nit is five seconds old; only `src/main/res` folders of the project count, not libraries or the\nframework (`@android:`)."), encoding="utf-8")
print("done")
