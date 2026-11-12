package org.codirex.asm.feature.terminal;

import java.util.Locale;

/** Turns byte counts from the installation into text for the progress line. */
final class InstallProgress {

  private InstallProgress() {}

  /** The percentage, 0 to 100, or -1 if the total is unknown. */
  static int percent(long done, long total) {
    if (total <= 0) {
      return -1;
    }
    long value = done * 100 / total;
    return (int) Math.max(0, Math.min(100, value));
  }

  /** Megabytes with one decimal, such as {@code 12.5}; always a dot, whatever the locale. */
  static String megabytes(long bytes) {
    return String.format(Locale.ROOT, "%.1f", Math.max(0, bytes) / (1024.0 * 1024.0));
  }
}
