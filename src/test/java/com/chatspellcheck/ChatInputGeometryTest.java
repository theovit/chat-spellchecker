package com.chatspellcheck;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChatInputGeometryTest
{
	@Test
	public void identityWhenWidgetTextHasNoExtraChars()
	{
		assertEquals(3, ChatInputGeometry.mapOffset("hello world", "hello world", 3));
	}

	@Test
	public void caretAtEndShiftsOnlyTheEndOffset()
	{
		String typed = "hello";
		String widget = "hello*";

		assertEquals(0, ChatInputGeometry.mapOffset(typed, widget, 0));
		assertEquals(6, ChatInputGeometry.mapOffset(typed, widget, 5));
	}

	@Test
	public void caretMidStringShiftsOffsetsAfterIt()
	{
		String typed = "helloworld";
		String widget = "hello*world";

		assertEquals(4, ChatInputGeometry.mapOffset(typed, widget, 4));
		assertEquals(6, ChatInputGeometry.mapOffset(typed, widget, 5));
		assertEquals(11, ChatInputGeometry.mapOffset(typed, widget, 10));
	}
}
