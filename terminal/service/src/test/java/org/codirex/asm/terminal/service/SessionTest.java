package org.codirex.asm.terminal.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;

public final class SessionTest {

  /** Collects what a UI would receive. */
  private static final class Recorder implements Session.Listener {
    private final ByteArrayOutputStream text = new ByteArrayOutputStream();
    final List<Long> offsets = new ArrayList<>();
    final CountDownLatch exited = new CountDownLatch(1);
    volatile int exitCode = Integer.MIN_VALUE;
    int exits;
    boolean failing;

    @Override
    public synchronized void onOutput(int session, long offset, byte[] data) {
      if (failing) {
        throw new IllegalStateException("dead");
      }
      offsets.add(offset);
      text.write(data, 0, data.length);
    }

    @Override
    public synchronized void onExit(int session, int code) {
      exits++;
      exitCode = code;
      exited.countDown();
    }

    synchronized String text() {
      return new String(text.toByteArray(), StandardCharsets.UTF_8);
    }

    /** Waits until the text has at least this many characters. */
    boolean waitForLength(int length) throws InterruptedException {
      for (int i = 0; i < 400; i++) {
        if (text().length() >= length) {
          return true;
        }
        Thread.sleep(10);
      }
      return false;
    }
  }

  private final FakeProcess process = new FakeProcess();
  private Session session;

  private Session start() {
    session = new Session(7, process, 1024);
    session.start();
    return session;
  }

  @After
  public void tearDown() {
    if (session != null) {
      session.close();
    }
  }

  @Test
  public void liveOutputReachesAnAttachedListenerWithOffsets() throws Exception {
    start();
    Recorder ui = new Recorder();
    session.attach(ui, 0);
    process.print("abc");
    assertTrue(ui.waitForLength(3));
    process.print("de");
    assertTrue(ui.waitForLength(5));
    assertEquals("abcde", ui.text());
    assertEquals(0L, (long) ui.offsets.get(0));
    assertEquals(3L, (long) ui.offsets.get(1));
  }

  @Test
  public void aLateListenerGetsTheReplayThenLiveOutput() throws Exception {
    start();
    Recorder first = new Recorder();
    session.attach(first, 0);
    process.print("hello ");
    assertTrue(first.waitForLength(6));

    Recorder late = new Recorder();
    session.attach(late, 0);
    assertEquals("hello ", late.text());
    process.print("world");
    assertTrue(late.waitForLength(11));
    assertEquals("hello world", late.text());
  }

  @Test
  public void attachingFromAnOffsetSkipsWhatTheUiAlreadyHas() throws Exception {
    start();
    Recorder first = new Recorder();
    session.attach(first, 0);
    process.print("0123456789");
    assertTrue(first.waitForLength(10));

    Recorder second = new Recorder();
    session.attach(second, 6);
    assertEquals("6789", second.text());
    assertEquals(6L, (long) second.offsets.get(0));
  }

  @Test
  public void aReplayThatStartsLaterThanAskedShowsTheGap() throws Exception {
    session = new Session(1, process, 4);
    session.start();
    Recorder first = new Recorder();
    session.attach(first, 0);
    process.print("0123456789");
    assertTrue(first.waitForLength(10));

    Recorder late = new Recorder();
    session.attach(late, 0);
    assertEquals("6789", late.text());
    assertEquals(6L, (long) late.offsets.get(0)); // the UI sees 6 > 0 and clears its screen
  }

  @Test
  public void aDetachedListenerHearsNothingMore() throws Exception {
    start();
    Recorder ui = new Recorder();
    session.attach(ui, 0);
    process.print("a");
    assertTrue(ui.waitForLength(1));
    session.detach(ui);
    process.print("b");
    Recorder other = new Recorder();
    session.attach(other, 0);
    assertTrue(other.waitForLength(2));
    assertEquals("a", ui.text());
  }

  @Test
  public void aListenerThatFailsIsDroppedAndTheOthersContinue() throws Exception {
    start();
    Recorder dead = new Recorder();
    Recorder alive = new Recorder();
    session.attach(dead, 0);
    session.attach(alive, 0);
    dead.failing = true;
    process.print("x");
    assertTrue(alive.waitForLength(1));
    process.print("y");
    assertTrue(alive.waitForLength(2));
    assertEquals("xy", alive.text());
  }

  @Test
  public void exitIsReportedOnceWithTheCode() throws Exception {
    start();
    Recorder ui = new Recorder();
    session.attach(ui, 0);
    process.print("bye");
    process.exit(3);
    assertTrue(ui.exited.await(5, TimeUnit.SECONDS));
    assertEquals(3, ui.exitCode);
    assertEquals(1, ui.exits);
    assertEquals("bye", ui.text());
    assertTrue(session.hasExited());
  }

  @Test
  public void attachingAfterTheExitGetsTheReplayAndTheExit() throws Exception {
    start();
    process.print("done");
    process.exit(0);
    for (int i = 0; i < 400 && !session.hasExited(); i++) {
      Thread.sleep(10);
    }
    assertTrue(session.hasExited());
    Recorder late = new Recorder();
    session.attach(late, 0);
    assertEquals("done", late.text());
    assertEquals(0, late.exitCode);
  }

  @Test
  public void aProgramThatEndsWhileSomethingHoldsTheTerminalIsClosed() throws Exception {
    process.holdOutputOpen = true;
    start();
    Recorder ui = new Recorder();
    session.attach(ui, 0);
    process.exit(0); // the shell is gone, a background job keeps the output open
    assertTrue(ui.exited.await(5, TimeUnit.SECONDS));
    assertTrue("the terminal is released", process.closed);
  }

  @Test
  public void inputResizeAndSignalsGoToTheProgram() throws Exception {
    start();
    session.write("ls\n".getBytes(StandardCharsets.UTF_8));
    session.resize(40, 120);
    session.signal(2);
    assertEquals("ls\n", process.typed());
    assertEquals(40, process.rows);
    assertEquals(120, process.columns);
    assertEquals(2, process.lastSignal);
  }

  @Test
  public void closeEndsTheProgramAndReportsTheExit() throws Exception {
    start();
    Recorder ui = new Recorder();
    session.attach(ui, 0);
    session.close();
    assertTrue(ui.exited.await(5, TimeUnit.SECONDS));
    assertTrue(process.closed);
    assertFalse(ui.exitCode == 0);
  }
}
