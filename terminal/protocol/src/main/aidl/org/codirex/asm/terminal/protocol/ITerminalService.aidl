package org.codirex.asm.terminal.protocol;

import org.codirex.asm.terminal.protocol.IInstallListener;
import org.codirex.asm.terminal.protocol.ITerminalListener;

/** The terminal service as the UI process sees it (ADR-0024). */
interface ITerminalService {
    /** Whether the userland (the shell and the packages) is installed. */
    boolean isUserlandInstalled();

    /**
     * Downloads and installs the userland, reporting to {@code listener}. If it is installed, only
     * {@code onDone} is called. Several calls at the same time share one installation.
     */
    void installUserland(IInstallListener listener);

    /** Stops a running installation; its listeners get {@code onFailed("CANCELLED", ...)}. */
    void cancelInstall();

    /**
     * Opens a session running the login shell in {@code directory} (the home directory if it is
     * empty or does not exist) and returns its id. Throws IllegalStateException with the reason if
     * the shell cannot start, for example because the userland is not installed.
     */
    int openSession(String directory, int rows, int columns);

    /** The ids of the sessions that exist, oldest first. */
    int[] sessionIds();

    /**
     * Starts delivering a session's output to {@code listener}: first the part of it from
     * {@code fromOffset} on that the service still has, then everything new. Throws
     * IllegalArgumentException for an unknown session.
     */
    void attach(int session, ITerminalListener listener, long fromOffset);

    void detach(int session, ITerminalListener listener);

    /** Input, as typed: Ctrl-C is the byte 3. Ignored for a session that is gone. */
    void write(int session, in byte[] data);

    void resize(int session, int rows, int columns);

    /** Sends a signal to the process group of the session. */
    void signal(int session, int signal);

    /** Ends the session if it runs and forgets it. */
    void closeSession(int session);
}
