package com.chatspellcheck;

import java.awt.Rectangle;
import net.runelite.api.FontTypeFace;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetTextAlignment;

/**
 * Computes the on-screen bounds of a substring of the chatbox input, shared by the overlay and
 * the right-click ignore menu so bounding-box logic isn't duplicated.
 *
 * The offsets passed in are into the raw typed text ({@link ChatInputTracker#currentTypedText}),
 * but the widget's rendered text isn't identical: public/clan chat ({@code Chatbox.INPUT}) wraps
 * it in a name/icon prefix and {@code <col=...>...</col>} formatting tags around both the typed
 * portion and the cursor glyph (e.g. {@code <img=2>Name: <col=0000ff>hello </col><col=0000ff>*</col>});
 * private messages ({@code Chatbox.MES_TEXT2}) just append the cursor directly
 * ({@code "hello*"}). Verified in-client for both: the raw typed text always appears as one
 * unbroken substring of the widget text, so its start is found with a plain substring search
 * rather than assuming a specific surrounding format.
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
		int textBlockStart = textBlockStart(inputWidget, font, widgetText, widgetBounds);

		return new Rectangle(textBlockStart + xBefore, widgetBounds.y, wordWidth, widgetBounds.height);
	}

	// Public chat's input line is left-aligned within its widget, but private-message text
	// (Chatbox.MES_TEXT2) is centered within a much wider container - assuming left-flush placed
	// the underline/suggestion well to the left of the actual glyphs (confirmed in-game).
	private static int textBlockStart(Widget inputWidget, FontTypeFace font, String widgetText, Rectangle widgetBounds)
	{
		int totalWidth = font.getTextWidth(widgetText);
		switch (inputWidget.getXTextAlignment())
		{
			case WidgetTextAlignment.CENTER:
				return widgetBounds.x + (widgetBounds.width - totalWidth) / 2;
			case WidgetTextAlignment.RIGHT:
				return widgetBounds.x + widgetBounds.width - totalWidth;
			default:
				return widgetBounds.x;
		}
	}

	static int findTypedTextStart(String widgetText, String typedText)
	{
		return widgetText.lastIndexOf(typedText);
	}
}
