package org.codirex.asm;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import org.codirex.asm.databinding.ActivityMainBinding;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.feature.filetree.FileTreeFragment;
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
    implements ProjectNavigator, FileTreeNavigator {

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
        .replace(R.id.container, FileTreeFragment.newInstance(name))
        .addToBackStack(null)
        .commit();
  }

  @Override
  public void onProjectCreated() {
    getSupportFragmentManager().popBackStack();
  }

  @Override
  public void openFile(ProjectName project, String relativePath) {
    // The editor does not exist yet; this is replaced when it does.
    Snackbar.make(
            binding.getRoot(),
            getString(R.string.opening_projects_soon, relativePath),
            Snackbar.LENGTH_SHORT)
        .show();
  }

  private void updateUpButton() {
    ActionBar bar = getSupportActionBar();
    if (bar != null) {
      bar.setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
    }
  }
}
