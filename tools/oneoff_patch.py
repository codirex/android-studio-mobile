D = 'terminal/view/src/main/java/org/codirex/asm/terminal/view/'

def patch(name, edits):
    p = D + name
    s = open(p).read()
    for old, new in edits:
        assert s.count(old) == 1, (name, old)
        s = s.replace(old, new)
    open(p, 'w').write(s)

patch('TerminalSelection.java', [
("""  void clear() {
    active = false;
  }

  private boolean anchorFirst() {
    return anchorRow < endRow || (anchorRow == endRow && anchorColumn <= endColumn);
  }
""", """  void clear() {
    active = false;
  }

  /** Moves the end that was fixed when the selection started (a handle of the selection). */
  void moveAnchor(int row, int column) {
    if (active) {
      anchorRow = row;
      anchorColumn = column;
    }
  }

  int anchorRow() {
    return anchorRow;
  }

  int anchorColumn() {
    return anchorColumn;
  }

  int endRow() {
    return endRow;
  }

  int endColumn() {
    return endColumn;
  }

  /** Whether the anchor is the first cell in reading order (else the free end is). */
  boolean isAnchorFirst() {
    return anchorFirst();
  }

  private boolean anchorFirst() {
    return anchorRow < endRow || (anchorRow == endRow && anchorColumn <= endColumn);
  }
"""),
])

patch('TerminalView.java', [
("        selecting = false;\n        copySelection();\n", "        selecting = false;\n        copySelection();\n        invalidate();\n"),
("  private boolean selecting;\n", """  private boolean selecting;

  private static final int HANDLE_NONE = 0;
  private static final int HANDLE_ANCHOR = 1;
  private static final int HANDLE_END = 2;

  /** The selection handle the finger is dragging, or {@link #HANDLE_NONE}. */
  private int dragHandle = HANDLE_NONE;
"""),
("""  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (selecting) {""", """  private float handleRadius() {
    return 9 * getResources().getDisplayMetrics().density;
  }

  /**
   * The centre of a selection handle in view coordinates, or null when its cell is off the screen.
   * The handle of the first cell hangs below the left edge of the cell, the other one below the
   * right edge. Call with the screen lock held.
   */
  private float[] handleCenter(int handle) {
    boolean anchor = handle == HANDLE_ANCHOR;
    int row = anchor ? selection.anchorRow() : selection.endRow();
    int column = anchor ? selection.anchorColumn() : selection.endColumn();
    int viewRow = row + scrollbackOffset();
    if (viewRow < 0 || viewRow >= rows) {
      return null;
    }
    boolean first = anchor == selection.isAnchorFirst();
    float x = first ? column * cellWidth : (column + 1) * cellWidth;
    return new float[] {x, (viewRow + 1) * cellHeight + handleRadius()};
  }

  /** Which handle is under the finger, or {@link #HANDLE_NONE}. Call with the lock held. */
  private int handleAt(float x, float y) {
    float reach = 28 * getResources().getDisplayMetrics().density;
    int best = HANDLE_NONE;
    float bestDistance = reach * reach;
    for (int handle = HANDLE_ANCHOR; handle <= HANDLE_END; handle++) {
      float[] center = handleCenter(handle);
      if (center == null) {
        continue;
      }
      float dx = x - center[0];
      float dy = y - center[1];
      float distance = dx * dx + dy * dy;
      if (distance <= bestDistance) {
        best = handle;
        bestDistance = distance;
      }
    }
    return best;
  }

  /** Drags the handle so the selection end sits at the cell the handle points to. */
  private void moveHandle(float x, float y) {
    synchronized (screen.lock()) {
      boolean anchor = dragHandle == HANDLE_ANCHOR;
      boolean first = anchor == selection.isAnchorFirst();
      // The handle hangs below its cell and at the cell's edge: aim the cell, not the finger.
      int row = rowAt(y - cellHeight / 2 - handleRadius());
      int column = columnAt(first ? x + cellWidth / 2 : x - cellWidth / 2);
      if (anchor) {
        selection.moveAnchor(row, column);
      } else {
        selection.extend(row, column);
      }
    }
    invalidate();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (dragHandle != HANDLE_NONE) {
      int action = event.getActionMasked();
      if (action == MotionEvent.ACTION_MOVE) {
        moveHandle(event.getX(), event.getY());
      } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        dragHandle = HANDLE_NONE;
        copySelection();
      }
      return true;
    }
    if (!selecting && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      int hit = HANDLE_NONE;
      synchronized (screen.lock()) {
        if (selection.isActive()) {
          hit = handleAt(event.getX(), event.getY());
        }
      }
      if (hit != HANDLE_NONE) {
        dragHandle = hit;
        flinger.forceFinished(true);
        getParent().requestDisallowInterceptTouchEvent(true);
        return true;
      }
    }
    if (selecting) {"""),
("""        canvas.drawRect(x, y, x + cellWidth, y + cellHeight, fillPaint);
      }
    }
  }
""", """        canvas.drawRect(x, y, x + cellWidth, y + cellHeight, fillPaint);
      }
      if (selection.isActive() && !selecting) {
        drawHandles(canvas);
      }
    }
  }

  /** Draws the two handles of the selection; call with the screen lock held. */
  private void drawHandles(Canvas canvas) {
    fillPaint.setColor(0xff4fc3f7);
    fillPaint.setAlpha(0xff);
    float radius = handleRadius();
    for (int handle = HANDLE_ANCHOR; handle <= HANDLE_END; handle++) {
      float[] center = handleCenter(handle);
      if (center != null) {
        canvas.drawCircle(center[0], center[1], radius, fillPaint);
      }
    }
  }
"""),
])
