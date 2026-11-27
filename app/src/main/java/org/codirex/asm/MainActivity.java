package org.codirex.asm;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.codirex.asm.databinding.ActivityMainBinding;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.toolchain.GradleTask;
import org.codirex.asm.feature.build.BuildFragment;
import org.codirex.asm.feature.build.BuildNavigator;
import org.codirex.asm.feature.editor.EditorNavigator;
import org.codirex.asm.feature.filetree.FileTreeNavigator;
import org.codirex.asm.feature.project.CreateProjectFragment;
import org.codirex.asm.feature.project.ProjectListFragment;
import org.codirex.asm.feature.project.ProjectNavigator;
import org.codirex.asm.feature.project.TemplateEditFragment;
import org.codirex.asm.feature.project.TemplateListFragment;
import org.codirex.asm.feature.sdk.SdkFragment;
import org.codirex.asm.feature.terminal.TerminalFragment;
import org.codirex.asm.feature.environment.EnvironmentFragment;
import org.codirex.asm.feature.toolchain.ToolchainFragment;
import org.codirex.asm.feature.toolchain.ToolchainNavigator;

/**
 * The single activity. It hosts the screens of the features and implements their navigation
 * contracts (ADR-0010), so features never reference each other.
 */
@AndroidEntryPoint
public final class MainActivity extends AppCompatActivity
    implements ProjectNavigator,
        FileTreeNavigator,
        EditorNavigator,
        ToolchainNavigator,
        BuildNavigator,
        CommandPaletteFragment.Host {

  private ActivityMainBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    setSupportActionBar(binding.toolbar);
    binding.projectChip.setOnClickListener(v -> showPalette());

    getSupportFragmentManager().addOnBackStackChangedListener(this::updateUpButton);
    if (savedInstanceState == null) {
      getSupportFragmentManager()
          .beginTransaction()
          .replace(R.id.container, new ProjectListFragment())
          .commit();
    }
    updateUpButton();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    // The terminal opens in the folder of the open project, so it is offered only inside one.
    MenuItem terminal = menu.findItem(R.id.action_terminal);
    if (terminal != null) {
      terminal.setVisible(workspace() != null);
    }
    MenuItem palette = menu.findItem(R.id.action_palette);
    if (palette != null) {
      palette.setVisible(workspace() != null);
    }
    MenuItem run = menu.findItem(R.id.action_run);
    if (run != null) {
      run.setVisible(workspace() != null);
    }
    // Gradle tasks run in the folder of the open project, like the terminal.
    MenuItem gradle = menu.findItem(R.id.action_gradle);
    if (gradle != null) {
      gradle.setVisible(workspace() != null);
    }
    // The template manager is offered on the project list, where a new project starts.
    MenuItem templates = menu.findItem(R.id.action_templates);
    if (templates != null) {
      Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
      templates.setVisible(current instanceof ProjectListFragment);
    }
    MenuItem toolchain = menu.findItem(R.id.action_toolchain);
    if (toolchain != null) {
      Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
      toolchain.setVisible(current instanceof ProjectListFragment);
    }
    MenuItem sdk = menu.findItem(R.id.action_sdk);
    if (sdk != null) {
      Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
      sdk.setVisible(current instanceof ProjectListFragment);
    }
    MenuItem environment = menu.findItem(R.id.action_environment);
    if (environment != null) {
      Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
      environment.setVisible(current instanceof ProjectListFragment);
    }
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_terminal) {
      showTerminal();
      return true;
    }
    if (item.getItemId() == R.id.action_palette) {
      showPalette();
      return true;
    }
    if (item.getItemId() == R.id.action_run) {
      runProject();
      return true;
    }
    if (item.getItemId() == R.id.action_gradle) {
      showGradleTasks();
      return true;
    }
    if (item.getItemId() == R.id.action_templates) {
      showTemplates();
      return true;
    }
    if (item.getItemId() == R.id.action_toolchain) {
      showToolchain();
      return true;
    }
    if (item.getItemId() == R.id.action_sdk) {
      showSdk();
      return true;
    }
    if (item.getItemId() == R.id.action_environment) {
      showEnvironment();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onSupportNavigateUp() {
    if (getSupportFragmentManager().popBackStackImmediate()) {
      return true;
    }
    return super.onSupportNavigateUp();
  }

  @Override
  public void showCreateProject() {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.container, new CreateProjectFragment())
        .addToBackStack(null)
        .commit();
  }

  @Override
  public void showProject(ProjectName name) {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.container, WorkspaceFragment.newInstance(name))
        .addToBackStack(null)
        .commit();
  }

  @Override
  public void onProjectCreated() {
    getSupportFragmentManager().popBackStack();
  }

  @Override
  public void showTemplates() {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.container, new TemplateListFragment())
        .addToBackStack(null)
        .commit();
  }

  @Override
  public void showTemplateEditor(String templateId) {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.container, TemplateEditFragment.newInstance(templateId))
        .addToBackStack(null)
        .commit();
  }

  @Override
  public void onTemplateSaved() {
    getSupportFragmentManager().popBackStack();
  }

  private void showEnvironment() {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.container, new EnvironmentFragment())
        .addToBackStack(null)
        .commit();
  }

  private void showSdk() {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.container, new SdkFragment())
        .addToBackStack(null)
        .commit();
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

  @Override
  public void openFile(ProjectName project, String relativePath) {
    WorkspaceFragment workspace = workspace();
    if (workspace != null) {
      workspace.openFile(relativePath);
    }
  }

  @Override
  public void pathDeleted(ProjectName project, String relativePath) {
    WorkspaceFragment workspace = workspace();
    if (workspace != null) {
      workspace.pathDeleted(relativePath);
    }
  }

  @Override
  public void pathRenamed(ProjectName project, String oldPath, String newPath) {
    WorkspaceFragment workspace = workspace();
    if (workspace != null) {
      workspace.pathRenamed(oldPath, newPath);
    }
  }

  @Override
  public void showBuild(String task) {
    if (workspace() != null && GradleTask.isValidName(task)) {
      runGradle(new GradleTask(task));
    }
  }

  @Override
  public void showFileTree() {
    WorkspaceFragment workspace = workspace();
    if (workspace != null) {
      workspace.showFiles();
    }
  }

  /** Opens a terminal in the folder of the open project (ADR-0012: files/projects/&lt;name&gt;). */
  void showTerminal() {
    WorkspaceFragment workspace = workspace();
    if (workspace == null) {
      return;
    }
    File directory = new File(getFilesDir(), "projects/" + workspace.project().value());
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.container, TerminalFragment.newInstance(directory.getPath()))
        .addToBackStack(null)
        .commit();
  }

  /**
   * Offers the common Gradle tasks of the open project and runs the chosen one on the build screen,
   * which shows the parsed output (ADR-0032).
   */
  void showGradleTasks() {
    if (workspace() == null) {
      return;
    }
    final List<GradleTask> tasks = GradleTask.common();
    CharSequence[] labels = new CharSequence[tasks.size()];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = tasks.get(i).name();
    }
    new AlertDialog.Builder(this)
        .setTitle(R.string.gradle_title)
        .setItems(labels, (dialog, which) -> runGradle(tasks.get(which)))
        .show();
  }

  private void runGradle(GradleTask task) {
    WorkspaceFragment workspace = workspace();
    if (workspace == null) {
      return;
    }
    File directory = new File(getFilesDir(), "projects/" + workspace.project().value());
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.container, BuildFragment.newInstance(directory.getPath(), task.name()))
        .addToBackStack(null)
        .commit();
  }

  /** Goes back from the build screen to the project that owns the file and opens it there. */
  @Override
  public void openBuildFile(String absolutePath, int line) {
    String root = new File(getFilesDir(), "projects").getPath() + "/";
    if (!absolutePath.startsWith(root)) {
      return;
    }
    String inside = absolutePath.substring(root.length());
    int slash = inside.indexOf('/');
    if (slash <= 0 || slash == inside.length() - 1) {
      return;
    }
    getSupportFragmentManager().popBackStackImmediate();
    WorkspaceFragment workspace = workspace();
    if (workspace != null && workspace.project().value().equals(inside.substring(0, slash))) {
      workspace.openFile(inside.substring(slash + 1), line);
    }
  }

  /** The Run button: builds the debug APK of the open project on the build screen. */
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

  /** Opens the command palette over the open project. */
  private void showPalette() {
    if (workspace() == null) {
      return;
    }
    new CommandPaletteFragment().show(getSupportFragmentManager(), "palette");
  }

  @Override
  public void paletteSymbols(
      java.util.function.Consumer<java.util.List<org.codirex.asm.domain.lsp.DocumentSymbol>> done) {
    WorkspaceFragment workspace = workspace();
    if (workspace == null) {
      done.accept(null);
    } else {
      workspace.symbols(done);
    }
  }

  @Override
  public void paletteWorkspaceSymbols(
      String query,
      java.util.function.Consumer<java.util.List<org.codirex.asm.domain.lsp.WorkspaceSymbol>> done) {
    WorkspaceFragment workspace = workspace();
    if (workspace == null) {
      done.accept(null);
    } else {
      workspace.workspaceSymbols(query, done);
    }
  }

  @Override
  public void paletteOpenFileAt(String relativePath, int line) {
    WorkspaceFragment workspace = workspace();
    if (workspace != null) {
      workspace.openFile(relativePath, line);
    }
  }

  @Override
  public void paletteGoTo(int line, int column) {
    WorkspaceFragment workspace = workspace();
    if (workspace != null) {
      workspace.goTo(line, column);
    }
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
            getString(R.string.palette_sync),
            "",
            R.drawable.ic_dock_build,
            () -> {
              WorkspaceFragment workspace = workspace();
              if (workspace != null) {
                workspace.sync();
              }
            }));
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
  private WorkspaceFragment workspace() {
    Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
    return current instanceof WorkspaceFragment ? (WorkspaceFragment) current : null;
  }

  private void updateUpButton() {
    ActionBar bar = getSupportActionBar();
    if (bar != null) {
      bar.setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
      WorkspaceFragment open = workspace();
      bar.setTitle(open != null ? "" : getString(R.string.app_name));
      binding.projectChip.setVisibility(open != null ? View.VISIBLE : View.GONE);
      if (open != null) {
        binding.projectChip.setText(open.project().value());
      }
    }
    invalidateOptionsMenu();
  }
}
