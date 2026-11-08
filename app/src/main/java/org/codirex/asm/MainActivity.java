package org.codirex.asm;

import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import org.codirex.asm.core.common.Asm;
import org.codirex.asm.core.concurrency.ApplicationRealm;
import org.codirex.clotho.Realm;

/** Placeholder screen. It only proves that Hilt and the application realm are wired. */
@AndroidEntryPoint
public final class MainActivity extends ComponentActivity {

  @Inject @ApplicationRealm Realm applicationRealm;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    TextView view = new TextView(this);
    int padding = (int) (24 * getResources().getDisplayMetrics().density);
    view.setPadding(padding, padding, padding, padding);
    view.setText(
        Asm.DISPLAY_NAME
            + "\n"
            + Asm.PACKAGE_NAME
            + "\nrealm: "
            + applicationRealm.name()
            + (applicationRealm.isClosing() ? " (closing)" : " (open)"));
    setContentView(view);
  }
}
