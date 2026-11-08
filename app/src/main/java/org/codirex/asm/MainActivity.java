package org.codirex.asm;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import org.codirex.asm.core.common.Asm;
import org.codirex.asm.core.concurrency.ApplicationRealm;
import org.codirex.asm.core.preferences.Pref;
import org.codirex.asm.core.preferences.PreferenceStore;
import org.codirex.asm.databinding.ActivityMainBinding;
import org.codirex.clotho.Realm;

/**
 * Placeholder screen. It only proves that the theme, view binding, Hilt, the application realm and
 * the preference store work together.
 */
@AndroidEntryPoint
public final class MainActivity extends AppCompatActivity {

  private static final Pref<Integer> LAUNCH_COUNT = Pref.ofInt("debug.launch_count", 0);

  @Inject @ApplicationRealm Realm applicationRealm;
  @Inject PreferenceStore preferences;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    if (savedInstanceState == null) {
      preferences.set(LAUNCH_COUNT, preferences.get(LAUNCH_COUNT) + 1);
    }

    binding.status.setText(
        Asm.PACKAGE_NAME
            + "\nrealm: "
            + applicationRealm.name()
            + (applicationRealm.isClosing() ? " (closing)" : " (open)")
            + "\nlaunches: "
            + preferences.get(LAUNCH_COUNT));
  }
}
