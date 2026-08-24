package com.chatspellcheck;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.FontTypeFace;
import net.runelite.api.Point;
import net.runelite.api.VarClientStr;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws an underline under each flagged word, a boxed suggestion label to the right of the word
 * currently being typed (if it's flagged), and the send-block confirmation banner. Only ever
 * reads state already computed by {@link ChatInputTracker} and {@link SendGuard} - never
 * re-tokenizes or re-checks the dictionary itself, so per-frame cost stays cheap.
 */
class ChatSpellcheckOverlay extends Overlay
{
	private static final Color UNDERLINE_COLOR = new Color(255, 64, 64);
	private static final Color BANNER_COLOR = new Color(255, 200, 0);
	private static final Color SUGGESTION_BG_COLOR = new Color(20, 20, 20, 235);
	private static final Color SUGGESTION_BORDER_COLOR = new Color(255, 180, 50);
	private static final Color SUGGESTION_TEXT_COLOR = new Color(255, 220, 150);
	private static final int SUGGESTION_PADDING_X = 5;
	private static final int SUGGESTION_PADDING_Y = 3;
	private static final int SUGGESTION_GAP = 6;

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
		Widget inputWidget = client.getWidget(InterfaceID.Chatbox.INPUT);
		if (inputWidget == null || inputWidget.isHidden())
		{
			return null;
		}

		String typedText = client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT);

		if (config.blockOnTypos() && sendGuard.isPendingConfirmation() && Objects.equals(typedText, sendGuard.getPendingText()))
		{
			renderConfirmBanner(graphics, inputWidget);
		}

		if (config.highlightMisspelledWords())
		{
			renderFlaggedWords(graphics, inputWidget, typedText);
		}

		return null;
	}

	private void renderFlaggedWords(Graphics2D graphics, Widget inputWidget, String typedText)
	{
		List<FlaggedWord> flagged = chatInputTracker.getFlaggedWords();
		if (flagged.isEmpty())
		{
			return;
		}

		FontTypeFace widgetFont = inputWidget.getFont();
		graphics.setColor(UNDERLINE_COLOR);

		WordToken lastToken = lastToken(typedText);
		FlaggedWord currentWord = null;
		Rectangle currentWordBounds = null;

		for (FlaggedWord word : flagged)
		{
			Rectangle bounds = ChatInputGeometry.boundsOf(inputWidget, typedText, word.getStartOffset(), word.getEndOffset());
			if (bounds == null)
			{
				continue;
			}

			int y = bounds.y + (widgetFont != null ? widgetFont.getBaseline() + 2 : bounds.height - 1);
			graphics.drawLine(bounds.x, y, bounds.x + bounds.width, y);

			if (lastToken != null && word.getStartOffset() == lastToken.getStartOffset() && word.getEndOffset() == lastToken.getEndOffset())
			{
				currentWord = word;
				currentWordBounds = bounds;
			}
		}

		if (currentWord != null && currentWord.getSuggestion() != null)
		{
			renderSuggestionBox(graphics, currentWordBounds, currentWord.getSuggestion());
		}
	}

	private static WordToken lastToken(String typedText)
	{
		List<WordToken> tokens = WordTokenizer.tokenize(typedText);
		return tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
	}

	private void renderSuggestionBox(Graphics2D graphics, Rectangle wordBounds, String suggestion)
	{
		Font font = FontManager.getRunescapeSmallFont();
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics(font);

		int textWidth = metrics.stringWidth(suggestion);
		int boxWidth = textWidth + SUGGESTION_PADDING_X * 2;
		int boxHeight = metrics.getAscent() + metrics.getDescent() + SUGGESTION_PADDING_Y * 2;
		int boxX = wordBounds.x + wordBounds.width + SUGGESTION_GAP;
		int boxY = wordBounds.y + (wordBounds.height - boxHeight) / 2;

		graphics.setColor(SUGGESTION_BG_COLOR);
		graphics.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 5, 5);
		graphics.setColor(SUGGESTION_BORDER_COLOR);
		graphics.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 5, 5);

		graphics.setColor(SUGGESTION_TEXT_COLOR);
		graphics.drawString(suggestion, boxX + SUGGESTION_PADDING_X, boxY + SUGGESTION_PADDING_Y + metrics.getAscent());
	}

	private void renderConfirmBanner(Graphics2D graphics, Widget inputWidget)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());
		Rectangle bounds = inputWidget.getBounds();
		Point location = new Point(bounds.x, bounds.y - 14);
		OverlayUtil.renderTextLocation(graphics, location, "Typos found - press Enter again to send", BANNER_COLOR);
	}
}
