package com.chatspellcheck;

import java.awt.Color;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;

/**
 * Adds a "add to spellcheck ignore list" entry when right-clicking a flagged word in the
 * chatbox input. Never adds entries that send an action to the server (MenuAction.RUNELITE is
 * handled entirely client-side).
 *
 * The menu's own background/panel is drawn natively by the client and isn't something a plugin
 * can restyle, but the option text color is - wrapped in the same {@code <col=RRGGBB>} tag OSRS
 * itself uses for menu entry text - so {@link ChatSpellcheckConfig#ignoreMenuColor()} controls
 * that.
 */
class SpellcheckMenuManager
{
	private final Client client;
	private final ChatInputTracker chatInputTracker;
	private final IgnoreListStore ignoreListStore;
	private final ChatSpellcheckConfig config;

	@Inject
	SpellcheckMenuManager(Client client, ChatInputTracker chatInputTracker, IgnoreListStore ignoreListStore, ChatSpellcheckConfig config)
	{
		this.client = client;
		this.chatInputTracker = chatInputTracker;
		this.ignoreListStore = ignoreListStore;
		this.config = config;
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (chatInputTracker.getFlaggedWords().isEmpty())
		{
			return;
		}

		Widget inputWidget = ChatInputTracker.currentInputWidget(client);
		if (inputWidget == null || inputWidget.isHidden())
		{
			return;
		}

		String typedText = ChatInputTracker.currentTypedText(client);
		Point mouse = client.getMouseCanvasPosition();

		for (FlaggedWord word : chatInputTracker.getFlaggedWords())
		{
			Rectangle bounds = ChatInputGeometry.boundsOf(inputWidget, typedText, word.getStartOffset(), word.getEndOffset());
			if (bounds == null || !bounds.contains(mouse.getX(), mouse.getY()))
			{
				continue;
			}

			client.getMenu().createMenuEntry(-1)
				.setOption(colorTag(config.ignoreMenuColor()) + "Add '" + word.getWord() + "' to spellcheck ignore list</col>")
				.setTarget("")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> onIgnoreClicked(word.getWord()));
			return;
		}
	}

	private void onIgnoreClicked(String word)
	{
		ignoreListStore.add(word);
		chatInputTracker.recompute(ChatInputTracker.currentTypedText(client));
	}

	private static String colorTag(Color color)
	{
		return String.format("<col=%02x%02x%02x>", color.getRed(), color.getGreen(), color.getBlue());
	}
}
