package com.chatspellcheck;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(IgnoreListStore.CONFIG_GROUP)
public interface ChatSpellcheckConfig extends Config
{
	@ConfigItem(
		keyName = "blockOnTypos",
		name = "Block send on typos",
		description = "Pressing Enter with likely typos present shows a confirmation instead of sending immediately.",
		position = 1
	)
	default boolean blockOnTypos()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightMisspelledWords",
		name = "Highlight misspelled words",
		description = "Underlines likely misspelled words in the chatbox input and shows a suggestion.",
		position = 2
	)
	default boolean highlightMisspelledWords()
	{
		return true;
	}

	@ConfigItem(
		keyName = IgnoreListStore.IGNORE_LIST_KEY,
		name = "Ignored words",
		description = "Words the spellchecker won't flag. Comma-separated - remove a word here to un-ignore it.",
		position = 3
	)
	default String ignoreList()
	{
		return "";
	}
}
