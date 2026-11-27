"""One-off: connects the Gradle sync to the editor, the activity, the workspace and the palette."""
import pathlib


def edit(path, pairs):
    p = pathlib.Path(path)
    s = p.read_text(encoding="utf-8")
    for old, new in pairs:
        assert s.count(old) == 1, (path, old[:80], s.count(old))
        s = s.replace(old, new)
    p.write_text(s, encoding="utf-8")


EDITOR = "feature/editor/src/main/java/org/codirex/asm/feature/editor/EditorFragment.java"
APP = "app/src/main/java/org/codirex/asm/"

edit("feature/editor/src/main/java/org/codirex/asm/feature/editor/EditorNavigator.java", [
    ("  void installPackage(String packageName);",
     "  void installPackage(String packageName);\n\n"
     "  /** The user asked to see a Gradle task run on the build screen (the details of a failed sync). */\n"
     "  void showBuild(String task);"),
])

edit(EDITOR, [
    ("    setUpQuickKeys();\n", "    setUpQuickKeys();\n    startSync(false);\n"),
    ("    JavaLanguageServer fileServer = serverFor(currentPath);\n",
     "    if (GradleSync.isRunning(projectDirectory)) {\n"
     "      binding.statusServer.setText(R.string.status_sync_running);\n"
     "      return;\n"
     "    }\n"
     "    JavaLanguageServer fileServer = serverFor(currentPath);\n"),
    ("  private void say(int message) {", r'''  /** Runs the Gradle sync now (the palette command), whether or not it is due. */
  public void runSync() {
    startSync(true);
  }

  /** Syncs the project's Gradle files for the Java server (ADR-0037), if forced or due. */
  private void startSync(boolean force) {
    if (binding == null || projectDirectory == null) {
      return;
    }
    if (!force && !GradleSync.needed(projectDirectory)) {
      return;
    }
    if (!GradleSync.isRunning(projectDirectory)) {
      GradleSync.start(
          requireContext(),
          projectDirectory,
          (ok, message) -> handler.post(() -> onSyncFinished(ok)));
    }
    updateStatus();
  }

  private void onSyncFinished(boolean ok) {
    if (binding == null) {
      return;
    }
    updateStatus();
    if (ok) {
      restartServer("java");
      say(R.string.sync_done);
      return;
    }
    Snackbar.make(binding.getRoot(), R.string.sync_failed, Snackbar.LENGTH_LONG)
        .setAction(
            R.string.sync_details, v -> ((EditorNavigator) requireActivity()).showBuild("asmSync"))
        .show();
  }

  /** Starts the server of a language anew, so that it reads the files the sync wrote. */
  private void restartServer(String language) {
    JavaLanguageServer old = servers.get(language);
    if (old == null || binding == null) {
      return;
    }
    old.setListener(null);
    old.stop();
    JavaLanguageServer fresh =
        new JavaLanguageServer(
            requireContext(),
            projectDirectory,
            workspaceKey(requireArguments().getString(EditorViewModel.ARG_PROJECT)),
            language);
    new ViewModelProvider(this).get(LanguageServerHolder.class).setServer(language, fresh);
    servers.put(language, fresh);
    fresh.setListener(listenerFor(language));
    if (language.equals(lspLanguage(currentPath))) {
      fresh.start();
      syncServerNow();
    }
  }

  private JavaLanguageServer.Listener listenerFor(final String language) {
    return new JavaLanguageServer.Listener() {
      @Override
      public void onState(JavaLanguageServer.State state) {
        handler.post(() -> onServerState(language, state));
      }

      @Override
      public void onDiagnostics(String uri, List<Diagnostic> found) {
        handler.post(() -> onServerDiagnostics(uri, found));
      }
    };
  }

  private void say(int message) {'''),
])

edit(APP + "WorkspaceFragment.java", [
    ("  /** Formats the shown file. */", r'''  /** Runs the Gradle sync of the project for the Java language server. */
  void sync() {
    EditorFragment editor = editor();
    if (editor != null) {
      editor.runSync();
    }
  }

  /** Formats the shown file. */'''),
])

edit(APP + "MainActivity.java", [
    ("  @Override\n  public void showFileTree() {", r'''  @Override
  public void showBuild(String task) {
    if (workspace() != null && GradleTask.isValidName(task)) {
      runGradle(new GradleTask(task));
    }
  }

  @Override
  public void showFileTree() {'''),
    ('            getString(R.string.palette_gradle), "", R.drawable.ic_dock_build, this::showGradleTasks));',
     r'''            getString(R.string.palette_gradle), "", R.drawable.ic_dock_build, this::showGradleTasks));
    items.add(
        new PaletteItem(
            getString(R.string.palette_sync),
            "",
            R.drawable.ic_dock_build,
            () -> {
              WorkspaceFragment workspace = workspace();
              if (workspace != null) {
                workspace.sync();
              }
            }));'''),
])
print("done")
