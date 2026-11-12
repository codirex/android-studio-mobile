package org.codirex.asm.terminal.protocol;

import org.codirex.asm.terminal.protocol.ITerminalListener;

/** The terminal service as the UI process sees it (ADR-0024). */
interface ITerminalService {
    /** Opens a session running the login shell in {@code directory}; returns its id. */
    int openSession(String directory, int rows, int columns);

    /** The ids of the sessions that exist, oldest first. */
    int[] sessionIds();

    /**
     * Starts delivering a session's output to {@code listener}: first the part of it from
     * {@code fromOffset} on that the service still has, then everything new.
     */
    void attach(int session, ITerminalListener listener, long fromOffset);

    void detach(int session, ITerminalListener listener);

    void write(int session, in byte[] data);

    void resize(int session, int rows, int columns);

    /** Sends a signal to the foreground process group of the session. */
    void signal(int session, int signal);

    /** Ends the session if it runs and forgets it. */
    void closeSession(int session);
}
