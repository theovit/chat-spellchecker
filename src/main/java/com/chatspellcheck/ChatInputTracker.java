package com.chatspellcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.client.eventbus.Subscribe;

/**
 * Recomputes the flagged-word list whenever the chatbox input text actually changes (never on a
 * tick or frame timer). Overlay rendering, the right-click ignore menu, and the send guard all
 * read {@link #getFlaggedWords()} rather than re-deriving it themselves.
 */
class ChatInputTracker
{
	private final Client client;
	private final SpellcheckDictionary dictionary;
	private final IgnoreListStore ignoreListStore;

	@Getter
	private volatile List<FlaggedWord> flaggedWords = Collections.emptyList();

	@Inject
	ChatInputTracker(Client client, SpellcheckDictionary dictionary, IgnoreListStore ignoreListStore)
	{
		this.client = client;
		this.dictionary = dictionary;
		this.ignoreListStore = ignoreListStore;
	}

	@Subscribe
	public void onVarClientStrChanged(VarClientStrChanged event)
	{
		if (event.getIndex() != VarClientStr.CHATBOX_TYPED_TEXT)
		{
			return;
		}

		recompute(client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT));
	}

	void reset()
	{
		flaggedWords = Collections.emptyList();
	}

	void recompute(String text)
	{
		if (!dictionary.isLoaded())
		{
			flaggedWords = Collections.emptyList();
			return;
		}

		List<WordToken> tokens = WordTokenizer.tokenize(text);
		List<FlaggedWord> flagged = new ArrayList<>();
		for (WordToken token : tokens)
		{
			String word = token.getWord();
			if (dictionary.isCorrect(word) || ignoreListStore.contains(word))
			{
				continue;
			}

			String suggestion = dictionary.suggest(word).orElse(null);
			flagged.add(new FlaggedWord(word, token.getStartOffset(), token.getEndOffset(), suggestion));
		}

		flaggedWords = flagged;
	}
}
