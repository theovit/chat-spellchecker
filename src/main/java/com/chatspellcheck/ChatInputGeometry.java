package com.chatspellcheck;

import java.awt.Rectangle;
import net.runelite.api.FontTypeFace;
import net.runelite.api.widgets.Widget;

/**
 * Computes the on-screen bounds of a substring of the chatbox input, shared by the overlay and
 * the right-click ignore menu so bounding-box logic isn't duplicated.
 *
 * The offsets passed in are into the raw typed text ({@code VarClientStr.CHATBOX_TYPED_TEXT}),
 * but the input widget's rendered text has a single cursor glyph spliced in at the caret
 * position, so offsets are remapped onto the widget text before measuring pixel width.
 */
final class ChatInputGeometry
{
	private ChatInputGeometry()
	{
	}

	static Rectangle boundsOf(Widget inputWidget, String typedText, int startOffset, int endOffset)
	{
		if (inputWidget == null || typedText == null)
		{
			return null;
		}

		FontTypeFace font = inputWidget.getFont();
		String widgetText = inputWidget.getText();
		Rectangle widgetBounds = inputWidget.getBounds();
		if (font == null || widgetText == null || widgetBounds == null)
		{
			return null;
		}

		if (startOffset < 0 || endOffset > typedText.length() || startOffset >= endOffset)
		{
			return null;
		}

		int widgetStart = mapOffset(typedText, widgetText, startOffset);
		int widgetEnd = mapOffset(typedText, widgetText, endOffset);
		if (widgetStart < 0 || widgetEnd > widgetText.length() || widgetStart >= widgetEnd)
		{
			return null;
		}

		int xBefore = font.getTextWidth(widgetText.substring(0, widgetStart));
		int wordWidth = font.getTextWidth(widgetText.substring(widgetStart, widgetEnd));

		return new Rectangle(widgetBounds.x + xBefore, widgetBounds.y, wordWidth, widgetBounds.height);
	}

	static int mapOffset(String typedText, String widgetText, int typedOffset)
	{
		int diff = widgetText.length() - typedText.length();
		if (diff <= 0)
		{
			return typedOffset;
		}

		int caretIndex = 0;
		int limit = Math.min(typedText.length(), widgetText.length());
		while (caretIndex < limit && typedText.charAt(caretIndex) == widgetText.charAt(caretIndex))
		{
			caretIndex++;
		}

		return typedOffset < caretIndex ? typedOffset : typedOffset + diff;
	}
}
