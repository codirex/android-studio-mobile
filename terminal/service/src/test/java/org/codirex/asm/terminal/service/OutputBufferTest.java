package org.codirex.asm.terminal.service;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public final class OutputBufferTest {

  private static byte[] b(String s) {
    return s.getBytes(StandardCharsets.US_ASCII);
  }

  private static String text(OutputBuffer.Chunk chunk) {
    return new String(chunk.bytes, StandardCharsets.US_ASCII);
  }

  @Test
  public void keepsEverythingUntilFull() {
    OutputBuffer buffer = new OutputBuffer(10);
    buffer.append(b("abc"), 0, 3);
    buffer.append(b("def"), 0, 3);
    assertEquals(6, buffer.end());
    assertEquals(0, buffer.start());
    assertEquals("abcdef", text(buffer.readFrom(0)));
    assertEquals("def", text(buffer.readFrom(3)));
  }

  @Test
  public void dropsTheOldestWhenItWraps() {
    OutputBuffer buffer = new OutputBuffer(4);
    buffer.append(b("abc"), 0, 3);
    buffer.append(b("defg"), 0, 4); // longer than what is left: wraps
    assertEquals(7, buffer.end());
    assertEquals(3, buffer.start());
    OutputBuffer.Chunk all = buffer.readFrom(0);
    assertEquals(3, all.offset);
    assertEquals("defg", text(all));
  }

  @Test
  public void aSingleWriteLargerThanTheBufferKeepsItsTail() {
    OutputBuffer buffer = new OutputBuffer(4);
    buffer.append(b("0123456789"), 0, 10);
    assertEquals(10, buffer.end());
    assertEquals("6789", text(buffer.readFrom(0)));
  }

  @Test
  public void aWriteAcrossTheEndOfTheArrayIsReadInOrder() {
    OutputBuffer buffer = new OutputBuffer(5);
    buffer.append(b("abcd"), 0, 4);
    buffer.append(b("efg"), 0, 3); // positions 4,0,1
    assertEquals("cdefg", text(buffer.readFrom(0)));
    assertEquals("fg", text(buffer.readFrom(5)));
  }

  @Test
  public void readingAtOrPastTheEndGivesNothing() {
    OutputBuffer buffer = new OutputBuffer(8);
    buffer.append(b("abc"), 0, 3);
    assertEquals(0, buffer.readFrom(3).bytes.length);
    assertEquals(0, buffer.readFrom(99).bytes.length);
  }

  @Test
  public void anOffsetIntoTheSourceArrayIsHonoured() {
    OutputBuffer buffer = new OutputBuffer(8);
    buffer.append(b("xxabcxx"), 2, 3);
    assertEquals("abc", text(buffer.readFrom(0)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void aZeroCapacityIsRefused() {
    new OutputBuffer(0);
  }
}
