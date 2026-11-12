package org.codirex.asm.terminal.protocol;

/** Follows the download and installation of the userland (ADR-0024). One way, like the output. */
oneway interface IInstallListener {
    /**
     * Progress. Stage 0 is the download ({@code done} and {@code total} are bytes, {@code total} is
     * -1 if the size is not known); stage 1 is the installation ({@code done} is the number of
     * files so far, {@code total} is -1).
     */
    void onProgress(int stage, long done, long total);

    /** The userland is installed. */
    void onDone();

    /**
     * The installation failed or was cancelled. {@code reason} is one of INVALID_ARCHIVE, STORAGE,
     * UNAVAILABLE, NETWORK, CHECKSUM, CANCELLED, UNKNOWN.
     */
    void onFailed(String reason, String message);
}
