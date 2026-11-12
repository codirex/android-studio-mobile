package org.codirex.asm.terminal.protocol;

/** Receives what a session prints (ADR-0024). Calls are one way: the service never waits for the UI. */
oneway interface ITerminalListener {
    /**
     * Output of a session. {@code offset} is the number of bytes the session had printed before
     * this chunk. A chunk that starts after the end of what the listener has seen means that older
     * output was dropped: the listener should clear its screen first.
     */
    void onOutput(int session, long offset, in byte[] data);

    /** The session has ended; {@code exitCode} is negative for a signal (minus its number). */
    void onExit(int session, int exitCode);
}
