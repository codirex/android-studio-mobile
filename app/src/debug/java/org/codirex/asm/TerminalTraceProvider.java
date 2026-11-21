package org.codirex.asm;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import org.codirex.trace.sdk.Trace;

/**
 * Debug builds only. Providers are created when their process starts, so declaring this one in the
 * {@code :terminal} process starts the Trace SDK there, and the logs of the shells and of jdt.ls
 * (which run in that process) reach the viewer too.
 */
public final class TerminalTraceProvider extends ContentProvider {

  @Override
  public boolean onCreate() {
    if (getContext() != null) {
      Trace.start(getContext());
    }
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    return null;
  }

  @Override
  public String getType(Uri uri) {
    return null;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return null;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    return 0;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return 0;
  }
}
