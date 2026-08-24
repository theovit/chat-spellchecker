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
 *
 * Since we can never insert the correction ourselves, the suggestion for the word currently being
 * typed "pins" once shown: backspacing the misspelled word keeps showing that last-good
 * suggestion (rather than flickering through whatever each half-deleted fragment would suggest on
 * its own), for as long as at least one character of the word remains. The instant it's fully
 * backspaced away, the suggestion drops immediately rather than hovering with nothing left to
 * anchor to. Retyping keeps the pin as long as what's typed is a prefix of it; typing something
 * that isn't - or completing it - clears the pin and lets a fresh suggestion take over. See
 * {@link #currentWordSuggestion}, {@link #currentWordSuggestionSince} (for the overlay's optional
 * auto-hide timeout), and {@link #currentWordSuggestionMatchedLength} (how much of it matches
 * what's typed so far, so the overlay can color the "already typed" prefix differently).
 */
@Singleton
class ChatInputTracker
{
	private final Client client;
	private final SpellcheckDictionary dictionary;
	private final IgnoreListStore ignoreListStore;

	@Getter
	private volatile List<FlaggedWord> flaggedWords = Collections.emptyList();

	/** The suggestion to display for the word currently being typed (see class doc for pinning). */
	@Getter
	private volatile String currentWordSuggestion;

	/** When {@link #currentWordSuggestion} last changed value - lets the overlay time out a stale one. */
	@Getter
	private volatile long currentWordSuggestionSince;

	/** How many leading characters of {@link #currentWordSuggestion} match what's typed so far. */
	@Getter
	private volatile int currentWordSuggestionMatchedLength;

	private volatile String lastText = "";
	private volatile String lastTrailingWord = "";
	private volatile String lastTrailingWordFreshSuggestion;
	private volatile String pinnedSuggestion;

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
		lastTrailingWord = "";
		lastTrailingWordFreshSuggestion = null;
		pinnedSuggestion = null;
		flaggedWords = Collections.emptyList();
		setCurrentWordSuggestion(null);
	}

	void recompute(String text)
	{
		if (!dictionary.isLoaded())
		{
			flaggedWords = Collections.emptyList();
			setCurrentWordSuggestion(null);
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
		updateCurrentWordSuggestion(text, tokens, flagged);
	}

	private void updateCurrentWordSuggestion(String text, List<WordToken> tokens, List<FlaggedWord> flagged)
	{
		// The "trailing word" is whatever's being actively composed at the very end of the input
		// (an empty string once it's been fully backspaced away, or if the text ends in a
		// separator like a space).
		boolean endsWithWordChar = !text.isEmpty() && !tokens.isEmpty() && tokens.get(tokens.size() - 1).getEndOffset() == text.length();
		WordToken trailingToken = endsWithWordChar ? tokens.get(tokens.size() - 1) : null;
		String trailingWord = trailingToken != null ? trailingToken.getWord() : "";

		String freshSuggestion = null;
		if (trailingToken != null)
		{
			for (FlaggedWord word : flagged)
			{
				if (word.getStartOffset() == trailingToken.getStartOffset() && word.getEndOffset() == trailingToken.getEndOffset())
				{
					freshSuggestion = word.getSuggestion();
					break;
				}
			}
		}

		if (trailingWord.isEmpty())
		{
			// Fully backspaced away (or nothing typed): drop the suggestion immediately rather
			// than leaving it hovering with no word left to anchor it to.
			pinnedSuggestion = null;
			setCurrentWordSuggestion(null);
			lastTrailingWord = trailingWord;
			lastTrailingWordFreshSuggestion = null;
			return;
		}

		boolean shrinking = trailingWord.length() < lastTrailingWord.length();

		if (pinnedSuggestion != null)
		{
			// Still being backspaced away: keep the pin no matter what the shrinking fragment
			// looks like (it's misspelled precisely because it doesn't align with the
			// suggestion). Only once the user starts typing again do we check consistency.
			if (!shrinking)
			{
				if (trailingWord.equalsIgnoreCase(pinnedSuggestion) || !pinnedSuggestion.startsWith(trailingWord.toLowerCase()))
				{
					pinnedSuggestion = null;
				}
			}
		}
		else if (shrinking && lastTrailingWordFreshSuggestion != null)
		{
			// Backspacing just started on a word that had a suggestion: pin it.
			pinnedSuggestion = lastTrailingWordFreshSuggestion;
		}

		setCurrentWordSuggestion(pinnedSuggestion != null ? pinnedSuggestion : freshSuggestion, trailingWord);
		lastTrailingWord = trailingWord;
		lastTrailingWordFreshSuggestion = freshSuggestion;
	}

	private void setCurrentWordSuggestion(String suggestion)
	{
		setCurrentWordSuggestion(suggestion, "");
	}

	private void setCurrentWordSuggestion(String suggestion, String trailingWord)
	{
		if (!Objects.equals(suggestion, currentWordSuggestion))
		{
			currentWordSuggestionSince = System.currentTimeMillis();
		}
		currentWordSuggestion = suggestion;
		currentWordSuggestionMatchedLength = (suggestion != null && suggestion.startsWith(trailingWord.toLowerCase()))
			? trailingWord.length() : 0;
	}
}
