import sys
p = 'terminal/view/src/main/java/org/codirex/asm/terminal/view/TerminalView.java'
s = open(p).read()

def rep(old, new):
    global s
    assert s.count(old) == 1, old
    s = s.replace(old, new)

rep("import android.view.inputmethod.InputMethodManager;\n",
    "import android.view.inputmethod.InputMethodManager;\nimport android.widget.OverScroller;\n")
rep("  private final ScaleGestureDetector scaling;\n",
    "  private final ScaleGestureDetector scaling;\n  private final OverScroller flinger;\n  private int flingLastY;\n")
rep("    screen = newScreen();\n    scaling =",
    "    screen = newScreen();\n    flinger = new OverScroller(context);\n    scaling =")
rep("""              public boolean onDown(MotionEvent e) {
                return true;
              }
""", """              public boolean onDown(MotionEvent e) {
                // A touch stops a fling that is still running.
                flinger.forceFinished(true);
                return true;
              }

              @Override
              public boolean onFling(MotionEvent first, MotionEvent second, float vx, float vy) {
                if (selecting) {
                  return false;
                }
                boolean scrollable;
                synchronized (screen.lock()) {
                  scrollable =
                      !screen.emulator().isAlternateScreen()
                          && screen.emulator().scrollbackSize() > 0;
                }
                if (!scrollable) {
                  return false;
                }
                flingLastY = 0;
                flinger.fling(
                    0, 0, 0, (int) vy, 0, 0, -FLING_RANGE, FLING_RANGE);
                postInvalidateOnAnimation();
                return true;
              }
""")
rep("  private void scrollBy(float distance) {",
"""  @Override
  public void computeScroll() {
    if (flinger.computeScrollOffset()) {
      int y = flinger.getCurrY();
      // A finger moving down (positive velocity) shows older lines, the opposite of scrollBy.
      scrollBy(flingLastY - y);
      flingLastY = y;
      postInvalidateOnAnimation();
    }
  }

  private void scrollBy(float distance) {""")
rep("  private static final int DEFAULT_FOREGROUND = 0xffd4d4d4;\n",
    "  /** The distance a fling may cover, in pixels: far more than any scrollback. */\n  private static final int FLING_RANGE = 1_000_000_000;\n\n  private static final int DEFAULT_FOREGROUND = 0xffd4d4d4;\n")
rep("  protected void onDetachedFromWindow() {\n    detach();\n",
    "  protected void onDetachedFromWindow() {\n    flinger.abortAnimation();\n    detach();\n")
open(p, 'w').write(s)
