def patch(path, edits):
    with open(path, encoding='utf-8') as f:
        s = f.read()
    for old, new in edits:
        assert s.count(old) == 1, (path, old)
        s = s.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(s)

patch('settings.gradle.kts', [
    ('include(":data:template")\n', 'include(":data:template")\ninclude(":data:toolchain")\n'),
    ('include(":domain:template")\n', 'include(":domain:template")\ninclude(":domain:toolchain")\n'),
    ('include(":feature:terminal")\n', 'include(":feature:terminal")\ninclude(":feature:toolchain")\n'),
])

patch('app/build.gradle.kts', [
    ('    implementation(project(":domain:template"))\n',
     '    implementation(project(":domain:template"))\n    implementation(project(":domain:toolchain"))\n'),
    ('    implementation(project(":data:template"))\n',
     '    implementation(project(":data:template"))\n    implementation(project(":data:toolchain"))\n'),
    ('    implementation(project(":feature:terminal"))\n',
     '    implementation(project(":feature:terminal"))\n    implementation(project(":feature:toolchain"))\n'),
])

patch('app/src/main/res/menu/main.xml', [(
'''    <item
        android:id="@+id/action_terminal"''',
'''    <item
        android:id="@+id/action_toolchain"
        android:title="@string/action_toolchain"
        android:visible="false"
        app:showAsAction="never" />

    <item
        android:id="@+id/action_terminal"''')])

patch('app/src/main/res/values/strings.xml', [
    ('</resources>', '    <string name="action_toolchain">Toolchain</string>\n</resources>')])
patch('app/src/main/res/values-ar/strings.xml', [
    ('</resources>', '    <string name="action_toolchain">أدوات التطوير</string>\n</resources>')])

patch('app/src/main/java/org/codirex/asm/MainActivity.java', [
    ('import org.codirex.asm.feature.terminal.TerminalFragment;\n',
     'import org.codirex.asm.feature.terminal.TerminalFragment;\n'
     'import org.codirex.asm.feature.toolchain.ToolchainFragment;\n'
     'import org.codirex.asm.feature.toolchain.ToolchainNavigator;\n'),
    ('    implements ProjectNavigator, FileTreeNavigator, EditorNavigator {\n',
     '    implements ProjectNavigator, FileTreeNavigator, EditorNavigator, ToolchainNavigator {\n'),
    ('''      templates.setVisible(current instanceof ProjectListFragment);
    }
''', '''      templates.setVisible(current instanceof ProjectListFragment);
    }
    MenuItem toolchain = menu.findItem(R.id.action_toolchain);
    if (toolchain != null) {
      Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
      toolchain.setVisible(current instanceof ProjectListFragment);
    }
'''),
    ('''    if (item.getItemId() == R.id.action_templates) {
      showTemplates();
      return true;
    }
''', '''    if (item.getItemId() == R.id.action_templates) {
      showTemplates();
      return true;
    }
    if (item.getItemId() == R.id.action_toolchain) {
      showToolchain();
      return true;
    }
'''),
    ('''  @Override
  public void onTemplateSaved() {
    getSupportFragmentManager().popBackStack();
  }
''', '''  @Override
  public void onTemplateSaved() {
    getSupportFragmentManager().popBackStack();
  }

  private void showToolchain() {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.container, new ToolchainFragment())
        .addToBackStack(null)
        .commit();
  }

  /** Installs a package of the userland by running the package manager in a terminal (ADR-0027). */
  @Override
  public void installPackage(String packageName) {
    // The names come from the domain's offers; the check keeps anything else out of the command.
    if (!packageName.matches("[a-z0-9][a-z0-9.+-]*")) {
      return;
    }
    File home = new File(getFilesDir(), "home");
    getSupportFragmentManager()
        .beginTransaction()
        .replace(
            R.id.container,
            TerminalFragment.newInstance(home.getPath(), "pkg install -y " + packageName))
        .addToBackStack(null)
        .commit();
  }
'''),
])

patch('feature/terminal/src/main/java/org/codirex/asm/feature/terminal/TerminalFragment.java', [
    ('  private static final String ARG_DIRECTORY = "directory";\n',
     '  private static final String ARG_DIRECTORY = "directory";\n  private static final String ARG_COMMAND = "command";\n'),
    ('''  /** A terminal that starts in {@code directory}. */
  public static TerminalFragment newInstance(String directory) {
    TerminalFragment fragment = new TerminalFragment();
    Bundle args = new Bundle();
    args.putString(ARG_DIRECTORY, directory);
    fragment.setArguments(args);
    return fragment;
  }
''', '''  /** A terminal that starts in {@code directory}. */
  public static TerminalFragment newInstance(String directory) {
    TerminalFragment fragment = new TerminalFragment();
    Bundle args = new Bundle();
    args.putString(ARG_DIRECTORY, directory);
    fragment.setArguments(args);
    return fragment;
  }

  /**
   * A terminal that starts in {@code directory} and runs {@code command} in a new session as soon
   * as it is shown, as if it were typed. The person sees the command and its output.
   */
  public static TerminalFragment newInstance(String directory, String command) {
    TerminalFragment fragment = newInstance(directory);
    fragment.requireArguments().putString(ARG_COMMAND, command);
    return fragment;
  }
'''),
    ('''  private void startSession() {
    loadSessions(-1, false);
  }''', '''  private void startSession() {
    // A command runs in a session of its own, not in one that is already busy.
    loadSessions(-1, requireArguments().containsKey(ARG_COMMAND));
  }'''),
    ('''    updatingTabs = false;
    showSession(selected);
  }''', '''    updatingTabs = false;
    showSession(selected);
    runPendingCommand();
  }

  /** Types the command this screen was asked to run, once; the session shows it and its output. */
  private void runPendingCommand() {
    Bundle args = requireArguments();
    String command = args.getString(ARG_COMMAND);
    if (command != null && binding != null) {
      args.remove(ARG_COMMAND);
      binding.terminal.typeText(command + "\\n");
    }
  }'''),
])

patch('terminal/service/src/main/java/org/codirex/asm/terminal/service/TerminalEnvironment.java', [
    ('''        env.put("LD_PRELOAD", file.getPath());
        break;
      }
    }
    return env;
  }
''', '''        env.put("LD_PRELOAD", file.getPath());
        break;
      }
    }
    File javaHome = javaHome();
    if (javaHome != null) {
      env.put("JAVA_HOME", javaHome.getPath());
    }
    return env;
  }

  /**
   * The newest JDK of the userland: a folder {@code lib/jvm/java-NN-...} with a {@code bin/java}.
   * {@code null} while no JDK is installed (ADR-0027).
   */
  File javaHome() {
    File[] folders = new File(prefix, "lib/jvm").listFiles();
    if (folders == null) {
      return null;
    }
    File best = null;
    int bestVersion = -1;
    for (File folder : folders) {
      int version = majorVersionOf(folder.getName());
      if (version > bestVersion && new File(folder, "bin/java").isFile()) {
        best = folder;
        bestVersion = version;
      }
    }
    return best;
  }

  /** 17 for {@code java-17-openjdk}; -1 for a name that is not a JDK folder. */
  static int majorVersionOf(String folderName) {
    if (!folderName.startsWith("java-")) {
      return -1;
    }
    int end = folderName.indexOf('-', 5);
    String digits = end < 0 ? folderName.substring(5) : folderName.substring(5, end);
    try {
      return Integer.parseInt(digits);
    } catch (NumberFormatException e) {
      return -1;
    }
  }
'''),
])

patch('docs/adr/README.md', [(
'| [0025](0025-terminal-view.md) | The terminal view | Proposed |\n',
'| [0025](0025-terminal-view.md) | The terminal view | Proposed |\n'
'| [0026](0026-textmate-highlighting.md) | Syntax highlighting with TextMate grammars | Proposed |\n'
'| [0027](0027-toolchain-jdk.md) | The toolchain: JDK management | Proposed |\n')])
