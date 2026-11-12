package org.codirex.asm.terminal.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public final class TerminalScreenTest {

  private static final class Events implements TerminalScreen.Callbacks {
    int changes;
    String title;
    int bells;
    Integer exit;
    final ByteArrayOutputStream replies = new ByteArrayOutputStream();

    @Override
    public void changed() {
      changes++;
    }

    @Override
    public void reply(byte[] data) {
      replies.write(data, 0, data.length);
    }

    @Override
    public void titleChanged(String title) {
      this.title = title;
    }

    @Override
    public void bell() {
      bells++;
    }

    @Override
    public void exited(int exitCode) {
      exit = exitCode;
    }
  }

  private final Events events = new Events();
  private final TerminalScreen screen = new TerminalScreen(5, 20, 100, events);

  private static byte[] b(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }

  private String text() {
    synchronized (screen.lock()) {
      return screen.emulator().screenText();
    }
  }

  @Test
  public void outputIsShownAndCounted() {
    screen.onOutput(0, b("hello"));
    screen.onOutput(5, b(" world"));
    assertEquals("hello world", text());
    assertEquals(11, screen.seenOffset());
    assertEquals(2, events.changes);
  }

  @Test
  public void aReplayThatOverlapsWhatWasSeenIsNotShownTwice() {
    screen.onOutput(0, b("abcdef"));
    screen.onOutput(3, b("defghi")); // starts inside what the screen has
    assertEquals("abcdefghi", text());
    assertEquals(9, screen.seenOffset());
  }

  @Test
  public void aChunkEntirelyBeforeTheSeenOffsetChangesNothing() {
    screen.onOutput(0, b("abcdef"));
    screen.onOutput(1, b("bcd"));
    assertEquals("abcdef", text());
    assertEquals(6, screen.seenOffset());
  }

  @Test
  public void aGapClearsTheScreenAndStartsFromTheNewOutput() {
    screen.onOutput(0, b("old text"));
    screen.onOutput(1000, b("new"));
    assertEquals("new", text());
    assertEquals(1003, screen.seenOffset());
  }

  @Test
  public void aFreshScreenAttachedToTrimmedOutputStartsClean() {
    screen.onOutput(500, b("tail"));
    assertEquals("tail", text());
    assertEquals(504, screen.seenOffset());
  }

  @Test
  public void theTerminalAnswersTheProgram() {
    screen.onOutput(0, b("abc\u001b[6n")); // where is the cursor?
    assertEquals("\u001b[1;4R", new String(events.replies.toByteArray(), StandardCharsets.US_ASCII));
  }

  @Test
  public void titleAndBellAreReported() {
    screen.onOutput(0, b("\u001b]0;my title\u0007\u0007"));
    assertEquals("my title", events.title);
    assertTrue(events.bells >= 1);
  }

  @Test
  public void exitIsKept() {
    assertFalse(screen.hasExited());
    screen.onExit(-9);
    assertTrue(screen.hasExited());
    assertEquals(-9, screen.exitCode());
    assertEquals(Integer.valueOf(-9), events.exit);
  }

  @Test
  public void resizeChangesTheEmulator() {
    screen.resize(10, 40);
    synchronized (screen.lock()) {
      assertEquals(10, screen.emulator().rows());
      assertEquals(40, screen.emulator().columns());
    }
  }

  @Test
  public void aSequenceSplitBetweenChunksStillWorks() {
    screen.onOutput(0, b("\u001b[3"));
    screen.onOutput(3, b("1mred"));
    synchronized (screen.lock()) {
      assertEquals('r', screen.emulator().codePointAt(0, 0));
      assertEquals(1, screen.emulator().foregroundAt(0, 0));
    }
  }
}
