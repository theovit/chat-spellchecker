package com.chatspellcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.client.eventbus.Subscribe;

/**
 * Recomputes the flagged-word list whenever the chatbox input text actually changes (never on a
 * tick or frame timer of our own). Overlay rendering, the right-click ignore menu, and the send
 * guard all read {@link #getFlaggedWords()} rather than re-deriving it themselves.
 *
 * Neither {@code VarClientStrChanged} nor a RuneLite {@code KeyListener} fires for chatbox
 * composition in this client (verified in-game: keystrokes are consumed by the client's own
 * input handling before RuneLite's KeyManager sees them, and the documented
 * {@code CHAT_TEXT_INPUT_REBUILD} clientscript never ran despite hundreds of other scripts
 * firing). {@link ScriptPostFired} does fire on essentially every client tick regardless of
 * script, so this treats it as a cheap "something happened, check the input" pulse: it re-reads
 * the var on every firing but only does the actual tokenize/dictionary work
 * ({@link #recompute}) when the text differs from what was last seen.
 */
@Singleton
class ChatInputTracker
{
	private final Client client;
	private final SpellcheckDictionary dictionary;
	private final IgnoreListStore ignoreListStore;

	@Getter
	private volatile List<FlaggedWord> flaggedWords = Collections.emptyList();

	private volatile String lastText = "";

	@Inject
	ChatInputTracker(Client client, SpellcheckDictionary dictionary, IgnoreListStore ignoreListStore)
	{
		this.client = client;
		this.dictionary = dictionary;
		this.ignoreListStore = ignoreListStore;
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		String text = client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT);
		if (text == null)
		{
			text = "";
		}

		if (!Objects.equals(text, lastText))
		{
			lastText = text;
			recompute(text);
		}
	}

	void reset()
	{
		lastText = "";
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
