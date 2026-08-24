package com.chatspellcheck;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;

/**
 * Persists user-added "don't flag this word" entries via {@link ConfigManager} under a hidden
 * (non-{@code @ConfigItem}) key, so they ride along with the user's RuneLite profile. No removal
 * UI in v1 — additive only.
 */
class IgnoreListStore
{
	static final String CONFIG_GROUP = "chat-spellcheck";
	static final String IGNORE_LIST_KEY = "ignoreList";
	private static final String DELIMITER = ",";

	private final ConfigManager configManager;
	private final Set<String> ignored = Collections.synchronizedSet(new LinkedHashSet<>());

	@Inject
	IgnoreListStore(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	void load()
	{
		String stored = configManager.getConfiguration(CONFIG_GROUP, IGNORE_LIST_KEY);
		ignored.clear();
		ignored.addAll(deserialize(stored));
	}

	boolean contains(String word)
	{
		return ignored.contains(word.toLowerCase());
	}

	void add(String word)
	{
		String lower = word.toLowerCase();
		if (ignored.add(lower))
		{
			configManager.setConfiguration(CONFIG_GROUP, IGNORE_LIST_KEY, serialize(ignored));
		}
	}

	static String serialize(Set<String> words)
	{
		return String.join(DELIMITER, words);
	}

	static Set<String> deserialize(String stored)
	{
		if (stored == null || stored.trim().isEmpty())
		{
			return Collections.emptySet();
		}

		Set<String> result = new LinkedHashSet<>();
		for (String word : stored.split(DELIMITER))
		{
			String trimmed = word.trim();
			if (!trimmed.isEmpty())
			{
				result.add(trimmed.toLowerCase());
			}
		}
		return result;
	}
}
