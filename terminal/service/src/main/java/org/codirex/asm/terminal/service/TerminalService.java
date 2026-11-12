package org.codirex.asm.terminal.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/**
 * The terminal service (ADR-0006, ADR-0024): a foreground service in its own process, which owns
 * the shells so that they outlive the UI. The UI starts it with {@link #start} and binds to it with
 * {@link #intent}; the Binder is {@code ITerminalService}.
 */
public final class TerminalService extends Service {

  private static final String CHANNEL_ID = "terminal";
  private static final int NOTIFICATION_ID = 1;

  private SessionManager sessions;
  private TerminalBinder binder;

  /** The intent that binds to the service. */
  public static Intent intent(Context context) {
    return new Intent(context, TerminalService.class);
  }

  /** Starts the service in the foreground; it keeps running until its last session is closed. */
  public static void start(Context context) {
    Intent intent = intent(context);
    if (Build.VERSION.SDK_INT >= 26) {
      context.startForegroundService(intent);
    } else {
      context.startService(intent);
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    sessions = new SessionManager(new PtyLauncher(getFilesDir()), SessionManager.DEFAULT_BUFFER);
    UserlandSetup setup = UserlandSetup.create(getFilesDir(), getCacheDir(), Build.SUPPORTED_ABIS);
    binder = new TerminalBinder(sessions, setup, this::stopSelf);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // A service started with startForegroundService must call startForeground at once.
    startForeground(NOTIFICATION_ID, notification());
    return START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public void onDestroy() {
    sessions.closeAll();
    super.onDestroy();
  }

  @SuppressWarnings("deprecation")
  private Notification notification() {
    Notification.Builder builder;
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationManager manager = getSystemService(NotificationManager.class);
      manager.createNotificationChannel(
          new NotificationChannel(
              CHANNEL_ID,
              getString(R.string.terminal_channel_name),
              NotificationManager.IMPORTANCE_LOW));
      builder = new Notification.Builder(this, CHANNEL_ID);
    } else {
      builder = new Notification.Builder(this);
    }
    return builder
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(getString(R.string.terminal_notification_title))
        .setOngoing(true)
        .build();
  }
}
