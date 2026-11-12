package org.codirex.asm;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.File;
import org.codirex.asm.databinding.ActivityMainBinding;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.feature.editor.EditorNavigator;
import org.codirex.asm.feature.filetree.FileTreeNavigator;
import org.codirex.asm.feature.project.CreateProjectFragment;
import org.codirex.asm.feature.project.ProjectListFragment;
import org.codirex.asm.feature.project.ProjectNavigator;
import org.codirex.asm.feature.terminal.TerminalFragment;

/**
 * The single activity. It hosts the screens of the features and implements their navigation
 * contracts (ADR-0010), so features never reference each other.
 */
@AndroidEntryPoint
public final class MainActivity extends AppCompatActivity
    implements ProjectNavigator, FileTreeNavigator, EditorNavigator {

  private ActivityMainBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    setSupportActionBar(binding.toolbar);

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
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_terminal) {
      showTerminal();
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
  public void showFileTree() {
    WorkspaceFragment workspace = workspace();
    if (workspace != null) {
      workspace.showFiles();
    }
  }

  /** Opens a terminal in the folder of the open project (ADR-0012: files/projects/&lt;name&gt;). */
  private void showTerminal() {
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

  /** The open project's workspace, or null when another screen is shown. */
  private WorkspaceFragment workspace() {
    Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
    return current instanceof WorkspaceFragment ? (WorkspaceFragment) current : null;
  }

  private void updateUpButton() {
    ActionBar bar = getSupportActionBar();
    if (bar != null) {
      bar.setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
    }
    invalidateOptionsMenu();
  }
}
