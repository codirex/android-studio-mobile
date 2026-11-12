package org.codirex.asm.terminal.service;

import android.os.IBinder;
import android.os.RemoteException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.codirex.asm.terminal.protocol.IInstallListener;
import org.codirex.asm.terminal.protocol.ITerminalListener;
import org.codirex.asm.terminal.protocol.ITerminalService;

/**
 * The Binder side of the terminal service (ADR-0024): checks the arguments of the UI process and
 * hands them to the sessions and to the userland setup.
 */
final class TerminalBinder extends ITerminalService.Stub {

  private final SessionManager sessions;
  private final UserlandSetup setup;
  private final Runnable onIdle;

  /** The listeners of the UI process, by session and Binder, so that detach finds them. */
  private final Map<Key, OutputRelay> relays = new HashMap<>();

  /**
   * @param onIdle called when the last session was closed; the service then may stop
   */
  TerminalBinder(SessionManager sessions, UserlandSetup setup, Runnable onIdle) {
    this.sessions = sessions;
    this.setup = setup;
    this.onIdle = onIdle;
  }

  @Override
  public boolean isUserlandInstalled() {
    return setup.isInstalled();
  }

  @Override
  public void installUserland(IInstallListener listener) {
    setup.start(new InstallRelay(listener));
  }

  @Override
  public void cancelInstall() {
    setup.cancel();
  }

  @Override
  public int openSession(String directory, int rows, int columns) {
    if (rows < 1 || columns < 1) {
      throw new IllegalArgumentException("bad terminal size " + rows + "x" + columns);
    }
    try {
      return sessions.open(directory, rows, columns);
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  @Override
  public int[] sessionIds() {
    return sessions.ids();
  }

  @Override
  public void attach(int session, ITerminalListener listener, long fromOffset) {
    Session found = sessions.get(session);
    if (found == null) {
      throw new IllegalArgumentException("no session " + session);
    }
    IBinder binder = listener.asBinder();
    Key key = new Key(session, binder);
    OutputRelay relay = new OutputRelay(key, listener);
    OutputRelay old;
    synchronized (relays) {
      old = relays.put(key, relay);
    }
    if (old != null) {
      // Outside the lock: a session holds its own lock while it calls a relay, and a relay then
      // takes the lock of the map.
      found.detach(old);
      old.unlink();
    }
    try {
      binder.linkToDeath(relay, 0);
    } catch (RemoteException e) {
      forget(key, relay);
      return;
    }
    found.attach(relay, Math.max(0, fromOffset));
  }

  @Override
  public void detach(int session, ITerminalListener listener) {
    Key key = new Key(session, listener.asBinder());
    OutputRelay relay;
    synchronized (relays) {
      relay = relays.remove(key);
    }
    if (relay != null) {
      Session found = sessions.get(session);
      if (found != null) {
        found.detach(relay);
      }
      relay.unlink();
    }
  }

  @Override
  public void write(int session, byte[] data) {
    Session found = sessions.get(session);
    if (found != null) {
      try {
        found.write(data);
      } catch (IOException ignored) {
        // The program has ended; the UI hears about it through onExit.
      }
    }
  }

  @Override
  public void resize(int session, int rows, int columns) {
    Session found = sessions.get(session);
    if (found != null && rows > 0 && columns > 0) {
      try {
        found.resize(rows, columns);
      } catch (IOException ignored) {
        // The terminal is gone.
      }
    }
  }

  @Override
  public void signal(int session, int signal) {
    Session found = sessions.get(session);
    if (found != null) {
      try {
        found.signal(signal);
      } catch (IOException ignored) {
        // The process is gone.
      }
    }
  }

  @Override
  public void closeSession(int session) {
    sessions.close(session);
    synchronized (relays) {
      relays.keySet().removeIf(key -> key.session == session);
    }
    if (sessions.ids().length == 0) {
      onIdle.run();
    }
  }

  private void forget(Key key, OutputRelay relay) {
    synchronized (relays) {
      if (relays.get(key) == relay) {
        relays.remove(key);
      }
    }
    relay.unlink();
  }

  private static final class Key {
    final int session;
    final IBinder binder;

    Key(int session, IBinder binder) {
      this.session = session;
      this.binder = binder;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Key)) {
        return false;
      }
      Key key = (Key) other;
      return session == key.session && binder.equals(key.binder);
    }

    @Override
    public int hashCode() {
      return 31 * session + binder.hashCode();
    }
  }

  /** Passes the output of a session to a listener in the UI process; drops it when that dies. */
  private final class OutputRelay implements Session.Listener, IBinder.DeathRecipient {
    private final Key key;
    private final ITerminalListener remote;

    OutputRelay(Key key, ITerminalListener remote) {
      this.key = key;
      this.remote = remote;
    }

    @Override
    public void onOutput(int session, long offset, byte[] data) {
      try {
        remote.onOutput(session, offset, data);
      } catch (RemoteException e) {
        forget(key, this);
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void onExit(int session, int exitCode) {
      try {
        remote.onExit(session, exitCode);
      } catch (RemoteException e) {
        forget(key, this);
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void binderDied() {
      Session found = sessions.get(key.session);
      if (found != null) {
        found.detach(this);
      }
      forget(key, this);
    }

    void unlink() {
      try {
        key.binder.unlinkToDeath(this, 0);
      } catch (RuntimeException ignored) {
        // Not linked, or already unlinked.
      }
    }
  }

  /** Passes the progress of the installation to a listener in the UI process. */
  private static final class InstallRelay implements UserlandSetup.Listener {
    private final IInstallListener remote;

    InstallRelay(IInstallListener remote) {
      this.remote = remote;
    }

    @Override
    public void onProgress(int stage, long done, long total) {
      try {
        remote.onProgress(stage, done, total);
      } catch (RemoteException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void onDone() {
      try {
        remote.onDone();
      } catch (RemoteException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void onFailed(String reason, String message) {
      try {
        remote.onFailed(reason, message);
      } catch (RemoteException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
