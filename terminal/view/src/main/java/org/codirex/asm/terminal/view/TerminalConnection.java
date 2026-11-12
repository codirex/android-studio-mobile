package org.codirex.asm.terminal.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import org.codirex.asm.terminal.protocol.ITerminalService;
import org.codirex.asm.terminal.service.TerminalService;

/**
 * The UI side of the connection to the terminal service (ADR-0025): starts the service, binds to
 * it and hands out its Binder. Callbacks run on the main thread.
 */
public final class TerminalConnection {

  /** Tells when the service can be used. */
  public interface Listener {
    void onConnected(ITerminalService service);

    /** The service process died; its sessions are gone. Connect again to start over. */
    void onDisconnected();
  }

  private final Context context;
  private final Listener listener;
  private boolean bound;

  private final ServiceConnection connection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
          listener.onConnected(ITerminalService.Stub.asInterface(binder));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
          listener.onDisconnected();
        }
      };

  public TerminalConnection(Context context, Listener listener) {
    this.context = context.getApplicationContext();
    this.listener = listener;
  }

  /** Starts the service and binds to it. Does nothing if already connecting. */
  public void connect() {
    if (bound) {
      return;
    }
    TerminalService.start(context);
    bound = context.bindService(TerminalService.intent(context), connection, Context.BIND_AUTO_CREATE);
  }

  /**
   * Lets go of the service. Its sessions keep running; connect again to reach them.
   */
  public void disconnect() {
    if (bound) {
      bound = false;
      context.unbindService(connection);
    }
  }
}
