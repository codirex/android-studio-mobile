package org.codirex.asm.domain.xmlassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class XmlContextTest {

  private static XmlContext atEnd(String text) {
    return XmlContext.at(text, text.length());
  }

  @Test
  public void afterTheTagNameAnAttributeNameStarts() {
    XmlContext c = atEnd("<LinearLayout android:id=\"@+id/a\" ");
    assertEquals(XmlContext.Kind.ATTRIBUTE_NAME, c.kind());
    assertEquals("LinearLayout", c.tag());
    assertEquals("", c.prefix());
    assertNull(c.parent());
    assertTrue(c.existing().contains("android:id"));
  }

  @Test
  public void aTypedAttributeNameIsThePrefixAndTheParentIsKnown() {
    XmlContext c = atEnd("<LinearLayout>\n  <TextView android:te");
    assertEquals(XmlContext.Kind.ATTRIBUTE_NAME, c.kind());
    assertEquals("TextView", c.tag());
    assertEquals("LinearLayout", c.parent());
    assertEquals("android:te", c.prefix());
  }

  @Test
  public void insideQuotesIsAValue() {
    XmlContext c = atEnd("<TextView android:textStyle=\"bo");
    assertEquals(XmlContext.Kind.ATTRIBUTE_VALUE, c.kind());
    assertEquals("android:textStyle", c.attribute());
    assertEquals("bo", c.prefix());
    assertTrue(c.quoted());
  }

  @Test
  public void rightAfterTheEqualsSignIsAnUnquotedValue() {
    XmlContext c = atEnd("<View android:visibility=");
    assertEquals(XmlContext.Kind.ATTRIBUTE_VALUE, c.kind());
    assertEquals("", c.prefix());
    assertEquals(false, c.quoted());
  }

  @Test
  public void rightAfterTheBracketIsATagName() {
    XmlContext c = atEnd("<Te");
    assertEquals(XmlContext.Kind.TAG_NAME, c.kind());
    assertEquals("Te", c.prefix());
    assertEquals(XmlContext.Kind.TAG_NAME, atEnd("<A>\n<").kind());
    assertEquals("A", atEnd("<A>\n<").parent());
  }

  @Test
  public void textAndEndTagsAreNothing() {
    assertEquals(XmlContext.Kind.NONE, atEnd("<A>hello").kind());
    assertEquals(XmlContext.Kind.NONE, atEnd("<A></A").kind());
    assertEquals(XmlContext.Kind.NONE, atEnd("").kind());
  }

  @Test
  public void commentsAreSkippedAndOpenCommentsAreNothing() {
    XmlContext c = atEnd("<!-- <X --><TextView ");
    assertEquals(XmlContext.Kind.ATTRIBUTE_NAME, c.kind());
    assertEquals("TextView", c.tag());
    assertEquals(XmlContext.Kind.NONE, atEnd("<!-- <TextView ").kind());
  }

  @Test
  public void aSelfClosedSiblingIsNotTheParent() {
    XmlContext c = atEnd("<LinearLayout><View /><Text");
    assertEquals(XmlContext.Kind.TAG_NAME, c.kind());
    assertEquals("LinearLayout", c.parent());
  }

  @Test
  public void aClosedSiblingIsNotTheParent() {
    XmlContext c = atEnd("<LinearLayout><FrameLayout></FrameLayout><View ");
    assertEquals("LinearLayout", c.parent());
  }
}
