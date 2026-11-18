def patch(path, edits):
    with open(path, encoding='utf-8') as f:
        s = f.read()
    for old, new in edits:
        assert s.count(old) == 1, (path, old)
        s = s.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(s)

patch('settings.gradle.kts', [
    ('include(":data:toolchain")\n', 'include(":data:environment")\ninclude(":data:toolchain")\n'),
    ('include(":domain:toolchain")\n', 'include(":domain:environment")\ninclude(":domain:toolchain")\n'),
    ('include(":feature:toolchain")\n', 'include(":feature:environment")\ninclude(":feature:toolchain")\n'),
])

patch('app/build.gradle.kts', [
    ('    implementation(project(":domain:toolchain"))\n',
     '    implementation(project(":domain:environment"))\n    implementation(project(":domain:toolchain"))\n'),
    ('    implementation(project(":data:toolchain"))\n',
     '    implementation(project(":data:environment"))\n    implementation(project(":data:toolchain"))\n'),
    ('    implementation(project(":feature:toolchain"))\n',
     '    implementation(project(":feature:environment"))\n    implementation(project(":feature:toolchain"))\n'),
])

patch('app/src/main/res/menu/main.xml', [(
'''    <item
        android:id="@+id/action_terminal"''',
'''    <item
        android:id="@+id/action_environment"
        android:title="@string/action_environment"
        android:visible="false"
        app:showAsAction="never" />

    <item
        android:id="@+id/action_terminal"''')])

patch('app/src/main/res/values/strings.xml', [
    ('</resources>', '    <string name="action_environment">Environment variables</string>\n</resources>')])
patch('app/src/main/res/values-ar/strings.xml', [
    ('</resources>', '    <string name="action_environment">متغيرات البيئة</string>\n</resources>')])

patch('app/src/main/java/org/codirex/asm/MainActivity.java', [
    ('import org.codirex.asm.feature.toolchain.ToolchainFragment;\n',
     'import org.codirex.asm.feature.environment.EnvironmentFragment;\n'
     'import org.codirex.asm.feature.toolchain.ToolchainFragment;\n'),
    ('''      toolchain.setVisible(current instanceof ProjectListFragment);
    }
''', '''      toolchain.setVisible(current instanceof ProjectListFragment);
    }
    MenuItem environment = menu.findItem(R.id.action_environment);
    if (environment != null) {
      Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
      environment.setVisible(current instanceof ProjectListFragment);
    }
'''),
    ('''    if (item.getItemId() == R.id.action_toolchain) {
      showToolchain();
      return true;
    }
''', '''    if (item.getItemId() == R.id.action_toolchain) {
      showToolchain();
      return true;
    }
    if (item.getItemId() == R.id.action_environment) {
      showEnvironment();
      return true;
    }
'''),
    ('''  private void showToolchain() {''', '''  private void showEnvironment() {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.container, new EnvironmentFragment())
        .addToBackStack(null)
        .commit();
  }

  private void showToolchain() {'''),
])

patch('docs/adr/README.md', [(
'| [0027](0027-toolchain-jdk.md) | The toolchain: JDK management | Proposed |\n',
'| [0027](0027-toolchain-jdk.md) | The toolchain: JDK management | Proposed |\n'
'| [0028](0028-environment-variables.md) | Custom environment variables of the terminal | Proposed |\n')])
