package org.codirex.asm;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import org.codirex.asm.core.common.Asm;

/** Placeholder entry point. It only proves that the module wiring and the toolchain work. */
public final class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView view = new TextView(this);
		view.setText(Asm.DISPLAY_NAME + "\n" + Asm.PACKAGE_NAME);
		view.setPadding(48, 48, 48, 48);
		setContentView(view);
	}
}
