package org.codirex.asm.domain.sdk;

/** Reports a download and lets the person cancel it. Called on the thread that downloads. */
public interface TransferProgress {

  /** {@code total} is -1 if the size is unknown. */
  void onProgress(long done, long total);

  /** Whether the download should stop. */
  boolean isCancelled();

  /** A progress that reports nothing and never cancels. */
  TransferProgress NONE =
      new TransferProgress() {
        @Override
        public void onProgress(long done, long total) {}

        @Override
        public boolean isCancelled() {
          return false;
        }
      };
}
