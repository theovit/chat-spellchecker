package com.chatspellcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
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
 *
 * Two different vars carry the typed text depending on mode, confirmed in-game:
 * {@code VarClientID.CHATINPUT} for public/clan/friends chat, and
 * {@code VarClientID.MESLAYERINPUT} for private messages (and other modal text entry, e.g. bank
 * search). {@link #currentTypedText} reads both and uses whichever is non-empty.
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
		String text = currentTypedText(client);

		if (!Objects.equals(text, lastText))
		{
			lastText = text;
			recompute(text);
		}
	}

	static String currentTypedText(Client client)
	{
		String chatbox = client.getVarcStrValue(VarClientID.CHATINPUT);
		if (chatbox != null && !chatbox.isEmpty())
		{
			return chatbox;
		}

		String mesLayer = client.getVarcStrValue(VarClientID.MESLAYERINPUT);
		return mesLayer != null ? mesLayer : "";
	}

	/**
	 * The widget that renders the current text-entry line. {@code Chatbox.INPUT} for
	 * public/clan/friends chat; private messages render into {@code Chatbox.MES_TEXT2} instead
	 * (confirmed in-game - {@code Chatbox.INPUT} stays hidden throughout PM composition).
	 */
	static Widget currentInputWidget(Client client)
	{
		Widget input = client.getWidget(InterfaceID.Chatbox.INPUT);
		if (input != null && !input.isHidden())
		{
			return input;
		}

		return client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
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
