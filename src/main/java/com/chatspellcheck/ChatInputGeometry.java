package com.chatspellcheck;

import java.awt.Rectangle;
import net.runelite.api.FontTypeFace;
import net.runelite.api.widgets.Widget;

/**
 * Computes the on-screen bounds of a substring of the chatbox input, shared by the overlay and
 * the right-click ignore menu so bounding-box logic isn't duplicated.
 *
 * The offsets passed in are into the raw typed text ({@code VarClientStr.CHATBOX_TYPED_TEXT}),
 * but the widget's rendered text additionally carries a name/icon prefix and
 * {@code <col=...>...</col>} formatting tags around the typed portion and the cursor glyph (e.g.
 * {@code <img=2>Name: <col=0000ff>hello </col><col=0000ff>*</col>}). Verified in-client: the raw
 * typed text always appears as one unbroken substring of the widget text (the tags wrap around
 * it, never split it), so its start is found with a plain substring search rather than an
 * assumption about a single inserted cursor character.
 */
final class ChatInputGeometry
{
	private ChatInputGeometry()
	{
	}

	static Rectangle boundsOf(Widget inputWidget, String typedText, int startOffset, int endOffset)
	{
		if (inputWidget == null || typedText == null || typedText.isEmpty())
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

		int typedStartInWidget = findTypedTextStart(widgetText, typedText);
		if (typedStartInWidget < 0)
		{
			return null;
		}

		int widgetStart = typedStartInWidget + startOffset;
		int widgetEnd = typedStartInWidget + endOffset;
		if (widgetEnd > widgetText.length())
		{
			return null;
		}

		int xBefore = font.getTextWidth(widgetText.substring(0, widgetStart));
		int wordWidth = font.getTextWidth(widgetText.substring(widgetStart, widgetEnd));

		return new Rectangle(widgetBounds.x + xBefore, widgetBounds.y, wordWidth, widgetBounds.height);
	}

	static int findTypedTextStart(String widgetText, String typedText)
	{
		return widgetText.lastIndexOf(typedText);
	}
}
