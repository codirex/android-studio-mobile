package org.codirex.asm.terminal.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The sessions of the terminal service, by id (ADR-0024). */
public final class SessionManager {

  /** Starts the program of a new session. */
  interface Launcher {
    TerminalProcess start(String directory, int rows, int columns) throws IOException;
  }

  static final int DEFAULT_BUFFER = 1024 * 1024;

  private final Launcher launcher;
  private final int bufferCapacity;
  private final Map<Integer, Session> sessions = new LinkedHashMap<>();
  private int nextId = 1;

  SessionManager(Launcher launcher, int bufferCapacity) {
    this.launcher = launcher;
    this.bufferCapacity = bufferCapacity;
  }

  /** Opens a session and returns its id. */
  synchronized int open(String directory, int rows, int columns) throws IOException {
    TerminalProcess process = launcher.start(directory, rows, columns);
    Session session = new Session(nextId++, process, bufferCapacity);
    sessions.put(session.id(), session);
    session.start();
    return session.id();
  }

  synchronized int[] ids() {
    int[] out = new int[sessions.size()];
    int i = 0;
    for (Integer id : sessions.keySet()) {
      out[i++] = id;
    }
    return out;
  }

  /** The session, or null if there is none with this id. */
  synchronized Session get(int id) {
    return sessions.get(id);
  }

  /** Ends the session if it runs and forgets it. Unknown ids are ignored. */
  void close(int id) {
    Session session;
    synchronized (this) {
      session = sessions.remove(id);
    }
    if (session != null) {
      session.close();
    }
  }

  /** Ends every session (the service is going away). */
  void closeAll() {
    List<Session> all;
    synchronized (this) {
      all = new ArrayList<>(sessions.values());
      sessions.clear();
    }
    for (Session session : all) {
      session.close();
    }
  }
}
