package org.codirex.asm;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import dagger.hilt.android.AndroidEntryPoint;
import org.codirex.asm.databinding.ActivityMainBinding;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.feature.editor.EditorNavigator;
import org.codirex.asm.feature.filetree.FileTreeNavigator;
import org.codirex.asm.feature.project.CreateProjectFragment;
import org.codirex.asm.feature.project.ProjectListFragment;
import org.codirex.asm.feature.project.ProjectNavigator;

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
  public void showFileTree() {
    WorkspaceFragment workspace = workspace();
    if (workspace != null) {
      workspace.showFiles();
    }
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
  }
}
