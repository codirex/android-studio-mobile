package org.codirex.asm.terminal.view;

/** The text size while the person pinches: kept in range, applied in half steps. */
final class TextScale {

  static final float MIN_SP = 8f;
  static final float MAX_SP = 32f;

  private TextScale() {}

  /** {@code sp} scaled by {@code factor}, kept between {@link #MIN_SP} and {@link #MAX_SP}. */
  static float scaled(float sp, float factor) {
    if (!(factor > 0f) || Float.isInfinite(factor)) {
      return clamp(sp);
    }
    return clamp(sp * factor);
  }

  /** The size that is applied for a pinched size: the nearest half step, so layout is not redone for tiny changes. */
  static float step(float sp) {
    return Math.round(clamp(sp) * 2f) / 2f;
  }

  private static float clamp(float sp) {
    return Math.max(MIN_SP, Math.min(MAX_SP, sp));
  }
}
