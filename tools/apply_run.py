"""One-off: the Run button and the project title in MainActivity."""
import pathlib

p = pathlib.Path("app/src/main/java/org/codirex/asm/MainActivity.java")
s = p.read_text(encoding="utf-8")

METHOD = '''  /** The Run button: builds the debug APK of the open project on the build screen. */
  private void runProject() {
    GradleTask chosen = null;
    for (GradleTask task : GradleTask.common()) {
      if (task.name().equals("assembleDebug")) {
        chosen = task;
        break;
      }
    }
    if (chosen != null) {
      runGradle(chosen);
    } else {
      showGradleTasks();
    }
  }

'''

pairs = [
    ("    // Gradle tasks run in the folder of the open project, like the terminal.\n",
     "    MenuItem run = menu.findItem(R.id.action_run);\n    if (run != null) {\n      run.setVisible(workspace() != null);\n    }\n    // Gradle tasks run in the folder of the open project, like the terminal.\n"),
    ("    if (item.getItemId() == R.id.action_gradle) {\n",
     "    if (item.getItemId() == R.id.action_run) {\n      runProject();\n      return true;\n    }\n    if (item.getItemId() == R.id.action_gradle) {\n"),
    ("      bar.setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);\n",
     "      bar.setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);\n      WorkspaceFragment open = workspace();\n      bar.setTitle(open != null ? open.project().value() : getString(R.string.app_name));\n"),
    ("  /** The open project's workspace, or null when another screen is shown. */\n",
     METHOD + "  /** The open project's workspace, or null when another screen is shown. */\n"),
]
for old, new in pairs:
    assert s.count(old) == 1, old[:70]
    s = s.replace(old, new)
p.write_text(s, encoding="utf-8")
print("done")
