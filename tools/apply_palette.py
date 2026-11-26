"""One-off: connects the command palette to MainActivity, EditorFragment and WorkspaceFragment, and the app to domain:palette."""
import pathlib


def patch(path, pairs):
    p = pathlib.Path(path)
    s = p.read_text(encoding="utf-8")
    for old, new in pairs:
        assert s.count(old) == 1, (path, old[:80], s.count(old))
        s = s.replace(old, new)
    p.write_text(s, encoding="utf-8")


APP = "app/src/main/java/org/codirex/asm/"

patch("app/build.gradle.kts", [
    ('    implementation(project(":domain:filetree"))\n',
     '    implementation(project(":domain:filetree"))\n    implementation(project(":domain:palette"))\n'),
])

patch("feature/editor/src/main/java/org/codirex/asm/feature/editor/EditorFragment.java", [
    ("  private void say(int message) {", r'''  /** Format the shown file (the palette and the selection menu share this). */
  public void runFormat() {
    formatDocument();
  }

  /** Go to the definition of the symbol at the caret. */
  public void runDefinition() {
    goToDefinition();
  }

  /** Show what the language server says about the symbol at the caret. */
  public void runInfo() {
    showHover();
  }

  private void say(int message) {'''),
])

patch(APP + "WorkspaceFragment.java", [
    ("  private EditorFragment editor() {", r'''  /** Formats the shown file. */
  void format() {
    EditorFragment editor = editor();
    if (editor != null) {
      editor.runFormat();
    }
  }

  /** Goes to the definition of the symbol at the caret. */
  void definition() {
    EditorFragment editor = editor();
    if (editor != null) {
      editor.runDefinition();
    }
  }

  /** Shows information about the symbol at the caret. */
  void info() {
    EditorFragment editor = editor();
    if (editor != null) {
      editor.runInfo();
    }
  }

  private EditorFragment editor() {'''),
])

COMMANDS = r'''  /** Opens the command palette over the open project. */
  private void showPalette() {
    if (workspace() == null) {
      return;
    }
    new CommandPaletteFragment().show(getSupportFragmentManager(), "palette");
  }

  @Override
  public File paletteProjectDirectory() {
    WorkspaceFragment workspace = workspace();
    return workspace == null
        ? null
        : new File(getFilesDir(), "projects/" + workspace.project().value());
  }

  @Override
  public void paletteOpenFile(String relativePath) {
    WorkspaceFragment workspace = workspace();
    if (workspace != null) {
      workspace.openFile(relativePath);
    }
  }

  /** The commands of the palette. The editor commands do nothing when no file is shown. */
  @Override
  public List<PaletteItem> paletteCommands() {
    String server = getString(R.string.palette_language_server);
    List<PaletteItem> items = new ArrayList<>();
    items.add(
        new PaletteItem(
            getString(R.string.palette_run), "assembleDebug", R.drawable.ic_palette_run, this::runProject));
    items.add(
        new PaletteItem(
            getString(R.string.palette_format),
            server,
            R.drawable.ic_palette_format,
            () -> {
              WorkspaceFragment workspace = workspace();
              if (workspace != null) {
                workspace.format();
              }
            }));
    items.add(
        new PaletteItem(
            getString(R.string.palette_definition),
            server,
            R.drawable.ic_palette_definition,
            () -> {
              WorkspaceFragment workspace = workspace();
              if (workspace != null) {
                workspace.definition();
              }
            }));
    items.add(
        new PaletteItem(
            getString(R.string.palette_info),
            server,
            R.drawable.ic_palette_info,
            () -> {
              WorkspaceFragment workspace = workspace();
              if (workspace != null) {
                workspace.info();
              }
            }));
    items.add(
        new PaletteItem(
            getString(R.string.palette_gradle), "", R.drawable.ic_dock_build, this::showGradleTasks));
    items.add(
        new PaletteItem(
            getString(R.string.palette_terminal), "", R.drawable.ic_dock_terminal, this::showTerminal));
    items.add(
        new PaletteItem(
            getString(R.string.palette_files),
            "",
            R.drawable.ic_dock_files,
            () -> {
              WorkspaceFragment workspace = workspace();
              if (workspace != null) {
                workspace.showFiles();
              }
            }));
    items.add(new PaletteItem(getString(R.string.palette_sdk), "", R.drawable.ic_dock_build, this::showSdk));
    items.add(
        new PaletteItem(
            getString(R.string.palette_toolchain), "", R.drawable.ic_dock_build, this::showToolchain));
    items.add(
        new PaletteItem(
            getString(R.string.palette_environment),
            "",
            R.drawable.ic_dock_terminal,
            this::showEnvironment));
    return items;
  }

  /** The open project's workspace, or null when another screen is shown. */
'''

patch(APP + "MainActivity.java", [
    ("import java.util.List;\n", "import java.util.ArrayList;\nimport java.util.List;\n"),
    ("        BuildNavigator {\n", "        BuildNavigator,\n        CommandPaletteFragment.Host {\n"),
    ("    MenuItem run = menu.findItem(R.id.action_run);\n",
     "    MenuItem palette = menu.findItem(R.id.action_palette);\n    if (palette != null) {\n      palette.setVisible(workspace() != null);\n    }\n    MenuItem run = menu.findItem(R.id.action_run);\n"),
    ("    if (item.getItemId() == R.id.action_run) {\n",
     "    if (item.getItemId() == R.id.action_palette) {\n      showPalette();\n      return true;\n    }\n    if (item.getItemId() == R.id.action_run) {\n"),
    ("  /** The open project's workspace, or null when another screen is shown. */\n", COMMANDS),
])
print("done")
