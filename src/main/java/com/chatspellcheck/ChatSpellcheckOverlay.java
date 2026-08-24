package com.chatspellcheck;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
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
 * Draws an underline under each flagged word and a suggestion tooltip for the hovered one, plus
 * the send-block confirmation banner. Only ever reads state already computed by
 * {@link ChatInputTracker} and {@link SendGuard} - never re-tokenizes or re-checks the
 * dictionary itself, so per-frame cost stays cheap.
 */
class ChatSpellcheckOverlay extends Overlay
{
	private static final Color UNDERLINE_COLOR = new Color(255, 64, 64);
	private static final Color BANNER_COLOR = new Color(255, 200, 0);

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

		if (config.blockOnTypos() && sendGuard.isPendingConfirmation())
		{
			renderConfirmBanner(graphics, inputWidget);
		}

		if (config.highlightMisspelledWords())
		{
			renderFlaggedWords(graphics, inputWidget);
		}

		return null;
	}

	private void renderFlaggedWords(Graphics2D graphics, Widget inputWidget)
	{
		List<FlaggedWord> flagged = chatInputTracker.getFlaggedWords();
		if (flagged.isEmpty())
		{
			return;
		}

		String typedText = client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT);
		Point mouse = client.getMouseCanvasPosition();
		graphics.setColor(UNDERLINE_COLOR);

		FlaggedWord hovered = null;
		Rectangle hoveredBounds = null;

		for (FlaggedWord word : flagged)
		{
			Rectangle bounds = ChatInputGeometry.boundsOf(inputWidget, typedText, word.getStartOffset(), word.getEndOffset());
			if (bounds == null)
			{
				continue;
			}

			int y = bounds.y + bounds.height - 1;
			graphics.drawLine(bounds.x, y, bounds.x + bounds.width, y);

			if (bounds.contains(mouse.getX(), mouse.getY()))
			{
				hovered = word;
				hoveredBounds = bounds;
			}
		}

		if (hovered != null && hovered.getSuggestion() != null)
		{
			renderSuggestionTooltip(graphics, hoveredBounds, hovered.getSuggestion());
		}
	}

	private void renderSuggestionTooltip(Graphics2D graphics, Rectangle wordBounds, String suggestion)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());
		String text = "did you mean: " + suggestion + "?";
		Point location = new Point(wordBounds.x, wordBounds.y - 4);
		OverlayUtil.renderTextLocation(graphics, location, text, Color.WHITE);
	}

	private void renderConfirmBanner(Graphics2D graphics, Widget inputWidget)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());
		Rectangle bounds = inputWidget.getBounds();
		Point location = new Point(bounds.x, bounds.y - 14);
		OverlayUtil.renderTextLocation(graphics, location, "Typos found - press Enter again to send", BANNER_COLOR);
	}
}
