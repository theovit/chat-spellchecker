package com.chatspellcheck;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.FontTypeFace;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws an underline under each flagged word, a boxed suggestion label at the cursor for
 * {@link ChatInputTracker#getCurrentWordSuggestion()}, and a boxed "message blocked" notice
 * anchored at the input line. Only ever reads state already computed by {@link ChatInputTracker}
 * and {@link SendGuard} - never re-tokenizes or re-checks the dictionary itself, so per-frame
 * cost stays cheap.
 */
class ChatSpellcheckOverlay extends Overlay
{
	private static final int BOX_PADDING_X = 5;
	private static final int BOX_PADDING_Y = 3;
	private static final int BOX_GAP = 6;
	private static final long PM_BANNER_DURATION_MS = 3000;

	private final Client client;
	private final ChatInputTracker chatInputTracker;
	private final SendGuard sendGuard;
	private final ChatSpellcheckConfig config;

	@Inject
	ChatSpellcheckOverlay(Client client, ChatInputTracker chatInputTracker, SendGuard sendGuard, ChatSpellcheckConfig config)
	{
		this.client = client;
		this.chatInputTracker = chatInputTracker;
		this.sendGuard = sendGuard;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Widget inputWidget = ChatInputTracker.currentInputWidget(client);
		String typedText = ChatInputTracker.currentTypedText(client);
		boolean widgetUsable = inputWidget != null && !inputWidget.isHidden();

		if (config.blockOnTypos() && sendGuard.isPendingConfirmation()
			&& System.currentTimeMillis() - sendGuard.getPendingSince() < PM_BANNER_DURATION_MS)
		{
			// The client clears the input box on Enter regardless of whether we block the send
			// (confirmed in-game for both public/clan chat and private messages), and we can't
			// repopulate it ourselves - RuneLite's guidelines forbid programmatically inserting
			// text into the chatbox. So this is a timed notice rather than a persisted-text
			// comparison: it tells the player the message was blocked and to retype it, drawn
			// right at the (now-empty) input line rather than up in the chat history area.
			Rectangle anchor = bannerAnchor(inputWidget, widgetUsable, typedText);
			if (anchor != null)
			{
				renderConfirmBanner(graphics, anchor, "Typos found - message blocked, retype to send");
			}
		}

		if (widgetUsable && config.highlightMisspelledWords())
		{
			renderUnderlines(graphics, inputWidget, typedText);

			String suggestion = chatInputTracker.getCurrentWordSuggestion();
			boolean timedOut = config.suggestionTimeoutEnabled()
				&& System.currentTimeMillis() - chatInputTracker.getCurrentWordSuggestionSince() >= config.suggestionTimeoutSeconds() * 1000L;
			if (suggestion != null && !timedOut)
			{
				Rectangle cursor = ChatInputGeometry.cursorBounds(inputWidget, typedText);
				if (cursor != null)
				{
					renderSuggestionBox(graphics, cursor, suggestion, chatInputTracker.getCurrentWordSuggestionMatchedLength());
				}
			}
		}

		return null;
	}

	private void renderUnderlines(Graphics2D graphics, Widget inputWidget, String typedText)
	{
		List<FlaggedWord> flagged = chatInputTracker.getFlaggedWords();
		if (flagged.isEmpty())
		{
			return;
		}

		FontTypeFace widgetFont = inputWidget.getFont();
		graphics.setColor(config.underlineColor());

		for (FlaggedWord word : flagged)
		{
			Rectangle bounds = ChatInputGeometry.boundsOf(inputWidget, typedText, word.getStartOffset(), word.getEndOffset());
			if (bounds == null)
			{
				continue;
			}

			int y = bounds.y + (widgetFont != null ? widgetFont.getBaseline() + 2 : bounds.height - 1);
			graphics.drawLine(bounds.x, y, bounds.x + bounds.width, y);
		}
	}

	// Anchored to the cursor position rather than a specific word's bounds, since a pinned
	// suggestion (see ChatInputTracker) can still be showing after its word has been fully
	// backspaced away, when there's no FlaggedWord bounds left to hang it off of.
	private void renderSuggestionBox(Graphics2D graphics, Rectangle cursor, String suggestion, int matchedLength)
	{
		Font font = boxFont();
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics(font);

		Rectangle box = boxBounds(metrics, suggestion, cursor);
		drawBox(graphics, box, config.suggestionBackgroundColor(), config.suggestionBorderColor());

		String typedPart = suggestion.substring(0, matchedLength);
		String remainingPart = suggestion.substring(matchedLength);
		int textX = box.x + BOX_PADDING_X;
		int textY = box.y + BOX_PADDING_Y + metrics.getAscent();

		graphics.setColor(config.suggestionTypedColor());
		graphics.drawString(typedPart, textX, textY);

		graphics.setColor(config.suggestionRemainingColor());
		graphics.drawString(remainingPart, textX + metrics.stringWidth(typedPart), textY);
	}

	// Prefers the cursor position within the (now-empty, post-block) input line; falls back to
	// the whole chatbox container since the PM compose window can disappear entirely on Enter,
	// even when the send was blocked.
	private Rectangle bannerAnchor(Widget inputWidget, boolean widgetUsable, String typedText)
	{
		if (widgetUsable)
		{
			Rectangle cursor = ChatInputGeometry.cursorBounds(inputWidget, typedText);
			if (cursor != null)
			{
				return cursor;
			}
		}

		Widget fallback = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
		if (fallback == null)
		{
			return null;
		}

		Rectangle bounds = fallback.getBounds();
		return new Rectangle(bounds.x, bounds.y, 0, bounds.height);
	}

	private void renderConfirmBanner(Graphics2D graphics, Rectangle anchor, String text)
	{
		Font font = boxFont();
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics(font);

		Rectangle box = boxBounds(metrics, text, anchor);
		drawBox(graphics, box, config.blockedBannerBackgroundColor(), config.blockedBannerBorderColor());

		graphics.setColor(config.blockedBannerTextColor());
		graphics.drawString(text, box.x + BOX_PADDING_X, box.y + BOX_PADDING_Y + metrics.getAscent());
	}

	// Derived from the game's own bitmap font rather than a fixed small/regular/large choice, so
	// the configured size is a plain point value and every box scales continuously with it.
	private Font boxFont()
	{
		return FontManager.getRunescapeFont().deriveFont((float) config.suggestionFontSize());
	}

	private static Rectangle boxBounds(FontMetrics metrics, String text, Rectangle anchor)
	{
		int textWidth = metrics.stringWidth(text);
		int boxWidth = textWidth + BOX_PADDING_X * 2;
		int boxHeight = metrics.getAscent() + metrics.getDescent() + BOX_PADDING_Y * 2;
		int boxX = anchor.x + BOX_GAP;
		int boxY = anchor.y + (anchor.height - boxHeight) / 2;
		return new Rectangle(boxX, boxY, boxWidth, boxHeight);
	}

	private static void drawBox(Graphics2D graphics, Rectangle box, Color background, Color border)
	{
		graphics.setColor(background);
		graphics.fillRoundRect(box.x, box.y, box.width, box.height, 5, 5);
		graphics.setColor(border);
		graphics.drawRoundRect(box.x, box.y, box.width, box.height, 5, 5);
	}
}
