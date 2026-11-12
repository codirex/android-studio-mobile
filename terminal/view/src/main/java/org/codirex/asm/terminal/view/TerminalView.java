package org.codirex.asm.terminal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.RemoteException;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.codirex.asm.terminal.emulator.TerminalEmulator;
import org.codirex.asm.terminal.emulator.TextStyle;
import org.codirex.asm.terminal.protocol.ITerminalListener;
import org.codirex.asm.terminal.protocol.ITerminalService;

/**
 * Shows one terminal session (ADR-0025): draws the screen of a {@link TerminalScreen}, turns keys,
 * the soft keyboard and gestures into input for the session, and keeps the size of the session in
 * step with the size of the view.
 *
 * <p>Calls to the service go through one background thread, so that the main thread never waits for
 * another process and input keeps its order.
 */
public final class TerminalView extends View {

  /** What the owner of the view wants to hear about. Called on the main thread. */
  public interface Callbacks {
    void onTitleChanged(String title);

    void onBell();

    /** The session ended; {@code exitCode} is negative for a signal. */
    void onSessionExited(int exitCode);
  }

  private static final int SCROLLBACK_LINES = 2000;
  private static final float DEFAULT_TEXT_SP = 14f;
  private static final int DEFAULT_FOREGROUND = 0xffd4d4d4;
  private static final int DEFAULT_BACKGROUND = 0xff1e1e1e;

  private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint fillPaint = new Paint();
  private final char[] glyph = new char[2];
  private final GestureDetector gestures;

  private TerminalScreen screen;
  private Callbacks callbacks;
  private ExecutorService calls;

  private ITerminalService service;
  private int session = -1;
  private ITerminalListener listener;

  private float cellWidth;
  private float cellHeight;
  private float baseline;
  private int rows = 24;
  private int columns = 80;
  private int scrollOffset;
  private float scrollRemainder;

  private boolean ctrlLatched;
  private boolean altLatched;
  private int foreground = DEFAULT_FOREGROUND;
  private int background = DEFAULT_BACKGROUND;

  public TerminalView(Context context) {
    this(context, null);
  }

  public TerminalView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setFocusable(true);
    setFocusableInTouchMode(true);
    textPaint.setTypeface(Typeface.MONOSPACE);
    setTextSizeSp(DEFAULT_TEXT_SP);
    screen = newScreen();
    gestures =
        new GestureDetector(
            context,
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onDown(MotionEvent e) {
                return true;
              }

              @Override
              public boolean onSingleTapUp(MotionEvent e) {
                requestFocus();
                InputMethodManager manager =
                    (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (manager != null) {
                  manager.showSoftInput(TerminalView.this, 0);
                }
                performClick();
                return true;
              }

              @Override
              public boolean onScroll(MotionEvent first, MotionEvent second, float dx, float dy) {
                scrollBy(dy);
                return true;
              }
            });
  }

  private TerminalScreen newScreen() {
    return new TerminalScreen(
        rows,
        columns,
        SCROLLBACK_LINES,
        new TerminalScreen.Callbacks() {
          @Override
          public void changed() {
            postInvalidateOnAnimation();
          }

          @Override
          public void reply(byte[] data) {
            sendToSession(data);
          }

          @Override
          public void titleChanged(final String title) {
            post(
                () -> {
                  if (callbacks != null) {
                    callbacks.onTitleChanged(title);
                  }
                });
          }

          @Override
          public void bell() {
            post(
                () -> {
                  if (callbacks != null) {
                    callbacks.onBell();
                  }
                });
          }

          @Override
          public void exited(final int exitCode) {
            post(
                () -> {
                  if (callbacks != null) {
                    callbacks.onSessionExited(exitCode);
                  }
                });
          }
        });
  }

  public void setCallbacks(Callbacks callbacks) {
    this.callbacks = callbacks;
  }

  /** The text size in scaled pixels; the number of rows and columns follows. */
  public void setTextSizeSp(float sp) {
    float pixels = sp * getResources().getDisplayMetrics().scaledDensity;
    textPaint.setTextSize(pixels);
    cellWidth = textPaint.measureText("M");
    Paint.FontMetrics metrics = textPaint.getFontMetrics();
    cellHeight = (float) Math.ceil(metrics.descent - metrics.ascent);
    baseline = -metrics.ascent;
    if (getWidth() > 0) {
      applySize(getWidth(), getHeight());
    }
    invalidate();
  }

  public void setColors(int foreground, int background) {
    this.foreground = foreground;
    this.background = background;
    invalidate();
  }

  public int rows() {
    return rows;
  }

  public int columns() {
    return columns;
  }

  /**
   * Shows {@code session} of the service. The screen is rebuilt from the output the service kept.
   * A view shows one session at a time; attaching again detaches the earlier one.
   */
  public void attach(final ITerminalService service, final int session) {
    detach();
    this.service = service;
    this.session = session;
    this.screen = newScreen();
    this.scrollOffset = 0;
    screen.resize(rows, columns);
    final TerminalScreen target = screen;
    listener =
        new ITerminalListener.Stub() {
          @Override
          public void onOutput(int id, long offset, byte[] data) {
            target.onOutput(offset, data);
          }

          @Override
          public void onExit(int id, int exitCode) {
            target.onExit(exitCode);
          }
        };
    final ITerminalListener attached = listener;
    final int attachedRows = rows;
    final int attachedColumns = columns;
    execute(
        () -> {
          try {
            service.resize(session, attachedRows, attachedColumns);
            service.attach(session, attached, 0);
          } catch (RemoteException | RuntimeException e) {
            // The service is gone or the session was closed: the screen stays as it is.
          }
        });
    invalidate();
  }

  /** Stops showing the session. It keeps running in the service. */
  public void detach() {
    final ITerminalService oldService = service;
    final int oldSession = session;
    final ITerminalListener oldListener = listener;
    service = null;
    session = -1;
    listener = null;
    if (oldService != null && oldListener != null) {
      execute(
          () -> {
            try {
              oldService.detach(oldSession, oldListener);
            } catch (RemoteException | RuntimeException e) {
              // Nothing to detach from any more.
            }
          });
    }
  }

  // ---- input ----

  /** Sends a special key; see {@link KeyEncoder}. Ctrl and Alt latched by the caller apply once. */
  public void sendKey(int key) {
    boolean applicationCursor;
    synchronized (screen.lock()) {
      applicationCursor = screen.emulator().isApplicationCursorKeys();
    }
    send(KeyEncoder.key(key, ctrlLatched, altLatched, false, applicationCursor));
    ctrlLatched = false;
    altLatched = false;
  }

  /** Makes the next typed character a control character (the Ctrl button of an extra keys row). */
  public void setCtrlLatched(boolean latched) {
    ctrlLatched = latched;
  }

  public boolean isCtrlLatched() {
    return ctrlLatched;
  }

  public void setAltLatched(boolean latched) {
    altLatched = latched;
  }

  public boolean isAltLatched() {
    return altLatched;
  }

  /** Types text, as the soft keyboard or a paste does. */
  public void typeText(String text) {
    send(KeyEncoder.text(text, ctrlLatched, altLatched));
    ctrlLatched = false;
    altLatched = false;
  }

  /** Pastes text, wrapped for programs that asked for it. */
  public void paste(String text) {
    boolean bracketed;
    synchronized (screen.lock()) {
      bracketed = screen.emulator().isBracketedPaste();
    }
    send(KeyEncoder.paste(text, bracketed));
  }

  private void send(byte[] bytes) {
    if (bytes.length == 0) {
      return;
    }
    scrollOffset = 0;
    sendToSession(bytes);
    postInvalidateOnAnimation();
  }

  private void sendToSession(final byte[] bytes) {
    final ITerminalService target = service;
    final int id = session;
    if (target == null || id < 0) {
      return;
    }
    execute(
        () -> {
          try {
            target.write(id, bytes);
          } catch (RemoteException | RuntimeException e) {
            // The service is gone; the screen already shows that nothing more arrives.
          }
        });
  }

  private synchronized void execute(Runnable call) {
    if (calls == null) {
      calls = Executors.newSingleThreadExecutor();
    }
    calls.execute(call);
  }

  @Override
  public boolean onCheckIsTextEditor() {
    return true;
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    // Plain text without suggestions or composing, so that every character arrives as typed.
    outAttrs.inputType =
        InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_FLAG_NO_EXTRACT_UI;
    return new BaseInputConnection(this, false) {
      @Override
      public boolean commitText(CharSequence text, int newCursorPosition) {
        typeText(text.toString());
        return true;
      }

      @Override
      public boolean setComposingText(CharSequence text, int newCursorPosition) {
        typeText(text.toString());
        return true;
      }

      @Override
      public boolean finishComposingText() {
        return true;
      }

      @Override
      public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        for (int i = 0; i < beforeLength; i++) {
          sendKey(KeyEncoder.KEY_BACKSPACE);
        }
        return true;
      }

      @Override
      public boolean sendKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
          return handleKey(event.getKeyCode(), event);
        }
        return true;
      }
    };
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return handleKey(keyCode, event) || super.onKeyDown(keyCode, event);
  }

  private boolean handleKey(int keyCode, KeyEvent event) {
    int key = mapKey(keyCode);
    boolean ctrl = event.isCtrlPressed() || ctrlLatched;
    boolean alt = event.isAltPressed() || altLatched;
    if (key != 0) {
      boolean applicationCursor;
      synchronized (screen.lock()) {
        applicationCursor = screen.emulator().isApplicationCursorKeys();
      }
      send(KeyEncoder.key(key, ctrl, alt, event.isShiftPressed(), applicationCursor));
      ctrlLatched = false;
      altLatched = false;
      return true;
    }
    if (keyCode == KeyEvent.KEYCODE_BACK
        || keyCode == KeyEvent.KEYCODE_VOLUME_UP
        || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        || KeyEvent.isModifierKey(keyCode)) {
      return false;
    }
    int meta = event.getMetaState() & ~(KeyEvent.META_CTRL_MASK | KeyEvent.META_ALT_MASK);
    int unicode = event.getUnicodeChar(meta);
    if (unicode == 0 || (unicode & KeyCharacterMap.COMBINING_ACCENT) != 0) {
      return false;
    }
    send(KeyEncoder.text(new String(Character.toChars(unicode)), ctrl, alt));
    ctrlLatched = false;
    altLatched = false;
    return true;
  }

  private static int mapKey(int keyCode) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_DPAD_UP:
        return KeyEncoder.KEY_UP;
      case KeyEvent.KEYCODE_DPAD_DOWN:
        return KeyEncoder.KEY_DOWN;
      case KeyEvent.KEYCODE_DPAD_LEFT:
        return KeyEncoder.KEY_LEFT;
      case KeyEvent.KEYCODE_DPAD_RIGHT:
        return KeyEncoder.KEY_RIGHT;
      case KeyEvent.KEYCODE_MOVE_HOME:
        return KeyEncoder.KEY_HOME;
      case KeyEvent.KEYCODE_MOVE_END:
        return KeyEncoder.KEY_END;
      case KeyEvent.KEYCODE_PAGE_UP:
        return KeyEncoder.KEY_PAGE_UP;
      case KeyEvent.KEYCODE_PAGE_DOWN:
        return KeyEncoder.KEY_PAGE_DOWN;
      case KeyEvent.KEYCODE_INSERT:
        return KeyEncoder.KEY_INSERT;
      case KeyEvent.KEYCODE_FORWARD_DEL:
        return KeyEncoder.KEY_DELETE;
      case KeyEvent.KEYCODE_TAB:
        return KeyEncoder.KEY_TAB;
      case KeyEvent.KEYCODE_ENTER:
      case KeyEvent.KEYCODE_NUMPAD_ENTER:
        return KeyEncoder.KEY_ENTER;
      case KeyEvent.KEYCODE_ESCAPE:
        return KeyEncoder.KEY_ESCAPE;
      case KeyEvent.KEYCODE_DEL:
        return KeyEncoder.KEY_BACKSPACE;
      default:
        break;
    }
    if (keyCode >= KeyEvent.KEYCODE_F1 && keyCode <= KeyEvent.KEYCODE_F12) {
      return KeyEncoder.KEY_F1 + (keyCode - KeyEvent.KEYCODE_F1);
    }
    return 0;
  }

  // ---- size, scrolling and touch ----

  @Override
  protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
    super.onSizeChanged(width, height, oldWidth, oldHeight);
    applySize(width, height);
  }

  private void applySize(int width, int height) {
    int newColumns = Math.max(1, (int) (width / cellWidth));
    int newRows = Math.max(1, (int) (height / cellHeight));
    if (newColumns == columns && newRows == rows) {
      return;
    }
    columns = newColumns;
    rows = newRows;
    screen.resize(rows, columns);
    final ITerminalService target = service;
    final int id = session;
    final int r = rows;
    final int c = columns;
    if (target != null && id >= 0) {
      execute(
          () -> {
            try {
              target.resize(id, r, c);
            } catch (RemoteException | RuntimeException e) {
              // The service is gone.
            }
          });
    }
  }

  private void scrollBy(float distance) {
    int maximum;
    synchronized (screen.lock()) {
      maximum = screen.emulator().isAlternateScreen() ? 0 : screen.emulator().scrollbackSize();
    }
    // A finger moving up (positive distance) shows newer lines.
    scrollRemainder += distance;
    int lines = (int) (scrollRemainder / cellHeight);
    scrollRemainder -= lines * cellHeight;
    scrollOffset = Math.max(0, Math.min(maximum, scrollOffset - lines));
    invalidate();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return gestures.onTouchEvent(event) || super.onTouchEvent(event);
  }

  @Override
  public boolean performClick() {
    return super.performClick();
  }

  @Override
  protected void onDetachedFromWindow() {
    detach();
    synchronized (this) {
      if (calls != null) {
        calls.shutdown();
        calls = null;
      }
    }
    super.onDetachedFromWindow();
  }

  // ---- drawing ----

  @Override
  protected void onDraw(Canvas canvas) {
    canvas.drawColor(background);
    synchronized (screen.lock()) {
      TerminalEmulator emulator = screen.emulator();
      int offset = emulator.isAlternateScreen() ? 0 : Math.min(scrollOffset, emulator.scrollbackSize());
      int visible = Math.min(rows, emulator.rows());
      for (int r = 0; r < visible; r++) {
        drawRow(canvas, emulator, r - offset, r * cellHeight);
      }
      if (offset == 0 && emulator.isCursorVisible() && emulator.cursorRow() < visible) {
        fillPaint.setColor(foreground);
        fillPaint.setAlpha(0x99);
        float x = emulator.cursorColumn() * cellWidth;
        float y = emulator.cursorRow() * cellHeight;
        canvas.drawRect(x, y, x + cellWidth, y + cellHeight, fillPaint);
      }
    }
  }

  /** Draws one row of the emulator ({@code row} is negative in the scrollback) at {@code top}. */
  private void drawRow(Canvas canvas, TerminalEmulator emulator, int row, float top) {
    int width = Math.min(columns, emulator.columns());
    for (int column = 0; column < width; column++) {
      int attributes = emulator.attributesAt(row, column);
      int fg = TerminalPalette.argb(emulator.foregroundAt(row, column), foreground);
      int bg = TerminalPalette.argb(emulator.backgroundAt(row, column), background);
      if ((attributes & TextStyle.INVERSE) != 0) {
        int swap = fg;
        fg = bg;
        bg = swap;
      }
      float x = column * cellWidth;
      if (bg != background) {
        fillPaint.setColor(bg);
        fillPaint.setAlpha(0xff);
        canvas.drawRect(x, top, x + cellWidth, top + cellHeight, fillPaint);
      }
      int codePoint = emulator.codePointAt(row, column);
      if (codePoint <= ' ' || (attributes & TextStyle.INVISIBLE) != 0) {
        continue;
      }
      textPaint.setColor(fg);
      if ((attributes & TextStyle.FAINT) != 0) {
        textPaint.setAlpha(0x80);
      }
      textPaint.setFakeBoldText((attributes & TextStyle.BOLD) != 0);
      textPaint.setTextSkewX((attributes & TextStyle.ITALIC) != 0 ? -0.25f : 0f);
      int length = Character.toChars(codePoint, glyph, 0);
      canvas.drawText(glyph, 0, length, x, top + baseline, textPaint);
      if ((attributes & TextStyle.UNDERLINE) != 0) {
        float y = top + baseline + 2;
        canvas.drawLine(x, y, x + cellWidth, y, textPaint);
      }
      if ((attributes & TextStyle.STRIKETHROUGH) != 0) {
        float y = top + cellHeight / 2;
        canvas.drawLine(x, y, x + cellWidth, y, textPaint);
      }
    }
  }
}
