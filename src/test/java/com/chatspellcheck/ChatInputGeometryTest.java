package com.chatspellcheck;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChatInputGeometryTest
{
	@Test
	public void findsTypedTextWrappedInFormattingTags()
	{
		String widgetText = "<img=2>Name: <col=0000ff>hello </col><col=0000ff>*</col>";

		assertEquals(widgetText.indexOf("hello "), ChatInputGeometry.findTypedTextStart(widgetText, "hello "));
	}

	@Test
	public void findsTypedTextWithNoFormatting()
	{
		assertEquals(0, ChatInputGeometry.findTypedTextStart("hello world", "hello world"));
	}

	@Test
	public void missingReturnsNegative()
	{
		assertEquals(-1, ChatInputGeometry.findTypedTextStart("Press Enter to Chat...", "hello"));
	}

	@Test
	public void picksTheLastOccurrence()
	{
		String widgetText = "no: <col=0000ff>no</col>";

		assertEquals(widgetText.lastIndexOf("no"), ChatInputGeometry.findTypedTextStart(widgetText, "no"));
	}
}
